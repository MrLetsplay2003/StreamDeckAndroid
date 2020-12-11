package me.mrletsplay.streamdeckandroid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.converter.JSONConverter;
import me.mrletsplay.streamdeck.action.Action;
import me.mrletsplay.streamdeck.action.ActionType;
import me.mrletsplay.streamdeck.deck.ButtonState;
import me.mrletsplay.streamdeck.deck.StreamDeck;
import me.mrletsplay.streamdeck.deck.StreamDeckProfile;
import me.mrletsplay.streamdeckandroid.networking.DeckNetworking;
import me.mrletsplay.streamdeckandroid.ui.BitmapUtils;
import me.mrletsplay.streamdeckandroid.ui.DeckButton;

public class MainActivity extends Activity {

	private StreamDeck deck;
	private List<DeckButton> uiButtons;

	private GestureDetector detector;

	private void showSettings(BiConsumer<String, Integer> dataConsumer, Runnable onNoInput) {
		AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this)
				.setTitle("Settings");
		View v = getLayoutInflater().inflate(R.layout.settings, findViewById(R.id.layout_root), false);
		EditText hostField = v.findViewById(R.id.settings_host);
		if(DeckNetworking.getHost() != null) hostField.setText(DeckNetworking.getHost());
		EditText portField = v.findViewById(R.id.settings_port);
		portField.setText(String.valueOf(DeckNetworking.getPort()));
		b.setView(v);

		b.setPositiveButton("Okay", (dialog, which) -> {
			try {
				String host = hostField.getText().toString();
				int port = Integer.parseInt(portField.getText().toString());
				getSharedPreferences("settings", MODE_PRIVATE).edit()
						.putString("host", host)
						.putInt("port", port)
						.apply();
				if(dataConsumer != null) dataConsumer.accept(host, port);
			}catch(NumberFormatException e) {
				new AlertDialog.Builder(MainActivity.this)
						.setTitle("Error")
						.setMessage("Your input was invalid")
						.setPositiveButton("Okay", (d, w) -> {
							if(onNoInput != null) onNoInput.run();
						})
						.show();
			}
		});

		b.setNegativeButton("Cancel", (dialog, which) -> {
			if(onNoInput != null) onNoInput.run();
		});

		b.show();
	}

	private static final BiConsumer<String, Integer> CONNECT_TO_OTHER_HOST = (host, port) -> {
		DeckNetworking.close();
		DeckNetworking.setHost(host);
		DeckNetworking.setPort(port);
		DeckNetworking.setClosed(false);
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

			@Override
			public void onLongPress(MotionEvent e) {
				showSettings(CONNECT_TO_OTHER_HOST, null);
				// Toast.makeText(getApplicationContext(), "This is a very very very very long press", Toast.LENGTH_SHORT).show();
			}
		});

		LinearLayout rows = findViewById(R.id.rows);

		uiButtons = new ArrayList<>();

		int id = 0;
		for(int i = 0; i < rows.getChildCount(); i++) {
			View v = rows.getChildAt(i);
			if(v instanceof LinearLayout) { // Row
				LinearLayout row = (LinearLayout) v;
				for(int j = 0; j < row.getChildCount(); j++) {
					View v2 = row.getChildAt(j);
					if(v2 instanceof DeckButton) {
						DeckButton btn = (DeckButton) v2;
						final int fID = id;
						btn.setOnClickListener(view -> onPressed(fID));
						uiButtons.add(btn);
						id++;
					}
				}
			}
		}

		deck = new StreamDeck(id);

		SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
		if(!prefs.contains("host") || !prefs.contains("port")) {
			showSettings((host, port) -> {
				CONNECT_TO_OTHER_HOST.accept(host, port);
				startReceiving();
			}, null);
		}else {
			DeckNetworking.setHost(prefs.getString("host", null));
			DeckNetworking.setPort(prefs.getInt("port", 10238));
			startReceiving();
		}
	}

	private void startReceiving() {
		new Thread(() -> {
			try {
				DeckNetworking.requestUpdate();
				while (true) {
					String cmd = DeckNetworking.receiveCommand();
					if(cmd == null) continue;
					JSONObject data = new JSONObject(cmd);
					switch(data.getString("command")) {
						case "setProfiles":
						{
							deck.setProfiles(data.getJSONArray("profiles").stream()
									.map(obj -> JSONConverter.decodeObject((JSONObject) obj, StreamDeckProfile.class))
									.collect(Collectors.toList()));
							deck.selectProfile(deck.getProfiles().get(0));
							updateUI();
							break;
						}
						case "setButtonStates":
						{
							String profile = data.getString("profile");
							ButtonState[] states = data.getJSONArray("states").stream()
									.map(o -> JSONConverter.decodeObject((JSONObject) o, ButtonState.class))
									.toArray(ButtonState[]::new);

							System.out.println(Arrays.toString(states));

							deck.getProfile(profile).getButton(data.getInt("button")).setStates(states);
							updateUI();
							break;
						}
						case "createProfile":
						{
							String profile = data.getString("profile");
							deck.createNewProfile(profile);
							break;
						}
						case "renameProfile":
						{
							String profile = data.getString("profile");
							String newID = data.getString("newID");
							deck.getProfile(profile).setIdentifier(newID);
							break;
						}
						case "deleteProfile":
						{
							String profile = data.getString("profile");
							deck.deleteProfile(profile);
							updateUI();
							break;
						}
					}
				}
			}catch(RuntimeException e) {
				e.printStackTrace();
				runOnUiThread(() -> new AlertDialog.Builder(this)
						.setNegativeButton(R.string.exit, (dialog, i) -> finish())
						.setPositiveButton(R.string.reconnect, (dialog, i) -> startReceiving())
						.setNeutralButton("Settings", (dialog, i) -> showSettings((host, port) -> {
							CONNECT_TO_OTHER_HOST.accept(host, port);
							startReceiving();
						}, null))
						.setCancelable(false)
						.setMessage(R.string.connection_lost)
						.show());
				//runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
			}
		}).start();
	}

	private void updateUI() {
		runOnUiThread(() -> {
			StreamDeckProfile p = deck.getCurrentProfile();
			for(int i = 0; i < deck.getButtonCount(); i++) {
				if(p == null) {
					uiButtons.get(i).setBitmap(BitmapUtils.solidColor(Color.GRAY));
					continue;
				}

				ButtonState state = p.getButton(i).getCurrentState();
				if(state == null) {
					uiButtons.get(i).setBitmap(BitmapUtils.solidColor(Color.GRAY));
					continue;
				}

				byte[] bytes = state.getBitmap();
				if(bytes == null) {
					uiButtons.get(i).setBitmap(BitmapUtils.solidColor(Color.GRAY));
					continue;
				}
				uiButtons.get(i).setBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
			}
		});
	}

	private void onPressed(int id) {
		StreamDeckProfile p = deck.getCurrentProfile();
		if(p == null) return;
		ButtonState state = p.getButton(id).getCurrentState();
		if(state == null) return;
		Action action = state.getAction();
		p.getButton(id).changeState();
		updateUI();
		if(action != null) {
			if(action.getType() == ActionType.SET_PROFILE) {
				String profile = action.getData().getString("profile");
				if(deck.getProfile(profile) == null) return;
				deck.selectProfile(profile);
				updateUI();
				return;
			}

			new Thread(() -> {
				try {
					JSONObject o = new JSONObject();
					o.put("action", action.toJSON());
					DeckNetworking.sendCommand(o.toString());
				}catch(RuntimeException e) {
					runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
				}
			}).start();
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return detector.onTouchEvent(event);
	}
}