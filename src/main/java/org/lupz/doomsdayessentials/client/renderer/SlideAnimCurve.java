package org.lupz.doomsdayessentials.client.renderer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal animation reader for sliding.animation.json that drives vanilla PlayerModel bones.
 * Only a subset of bones/axes are supported to keep it simple.
 */
public class SlideAnimCurve {
	private static final ResourceLocation SLIDE_JSON = ResourceLocation.fromNamespaceAndPath("doomsdayessentials", "animations/sliding.animation.json");
	private static final Gson GSON = new Gson();

	private static volatile boolean loaded = false;
	private static final Map<Float, float[]> bodyXR = new LinkedHashMap<>(); // time -> [deg]
	private static final Map<Float, float[]> headXR = new LinkedHashMap<>();
	private static final Map<Float, float[]> leftArmXYZ = new LinkedHashMap<>(); // [x,y,z]
	private static final Map<Float, float[]> rightArmX = new LinkedHashMap<>(); // [x]
	private static float animLength = 0.5f; // default

	public static void ensureLoaded() {
		if (loaded) return;
		synchronized (SlideAnimCurve.class) {
			if (loaded) return;
			try {
				var resOpt = Minecraft.getInstance().getResourceManager().getResource(SLIDE_JSON);
				if (resOpt.isEmpty()) { loaded = true; return; }
				try (var is = resOpt.get().open(); var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
					JsonObject root = GSON.fromJson(br, JsonObject.class);
					JsonObject anims = root.getAsJsonObject("animations");
					JsonObject slide = anims.getAsJsonObject("animation.sliding");
					if (slide.has("animation_length")) animLength = slide.get("animation_length").getAsFloat();
					JsonObject bones = slide.getAsJsonObject("bones");
					readBoneRotation(bones, "Body", bodyXR, true, false);
					readBoneRotation(bones, "Head", headXR, true, false);
					readBoneRotation(bones, "LeftArm", leftArmXYZ, true, true); // x,y,z
					readBoneRotation(bones, "RightArm", rightArmX, true, false);
				}
			} catch (Exception ignored) {
			}
			loaded = true;
		}
	}

	private static void readBoneRotation(JsonObject bones, String boneName, Map<Float, float[]> out, boolean useX, boolean useYz) {
		if (!bones.has(boneName)) return;
		JsonObject bone = bones.getAsJsonObject(boneName);
		if (!bone.has("rotation")) return;
		JsonObject rot = bone.getAsJsonObject("rotation");
		for (Map.Entry<String, JsonElement> e : rot.entrySet()) {
			float t = Float.parseFloat(e.getKey());
			var arr = e.getValue().getAsJsonArray();
			float x = arr.size() > 0 ? arr.get(0).getAsFloat() : 0f;
			float y = arr.size() > 1 ? arr.get(1).getAsFloat() : 0f;
			float z = arr.size() > 2 ? arr.get(2).getAsFloat() : 0f;
			if (useYz) out.put(t, new float[]{x, y, z});
			else out.put(t, new float[]{x});
		}
	}

	private static float lerp(float a, float b, float k) { return a + (b - a) * k; }
	private static float degToRad(float d) { return (float) Math.toRadians(d); }

	private static float sample(Map<Float, float[]> map, float t, int idx) {
		if (map.isEmpty()) return 0f;
		Float prev = null; float[] prevV = null;
		Float next = null; float[] nextV = null;
		for (Map.Entry<Float, float[]> e : map.entrySet()) {
			float et = e.getKey();
			if (et <= t) { prev = et; prevV = e.getValue(); }
			if (et >= t) { next = et; nextV = e.getValue(); break; }
		}
		if (prev == null) { prev = next; prevV = nextV; }
		if (next == null) { next = prev; nextV = prevV; }
		if (prev == null || next == null || prevV == null || nextV == null) return 0f;
		if (prev.equals(next)) return prevV[Math.min(idx, prevV.length-1)];
		float k = (t - prev) / (next - prev);
		float a = prevV[Math.min(idx, prevV.length-1)];
		float b = nextV[Math.min(idx, nextV.length-1)];
		return lerp(a, b, k);
	}

	public static void apply(PlayerModel<?> model, float timeSec) {
		ensureLoaded();
		float t = animLength <= 0 ? 0f : (timeSec % animLength);
		float bx = degToRad(sample(bodyXR, t, 0));
		float hx = degToRad(sample(headXR, t, 0));
		float lax = degToRad(sample(leftArmXYZ, t, 0));
		float lay = degToRad(sample(leftArmXYZ, t, 1));
		float laz = degToRad(sample(leftArmXYZ, t, 2));
		float rax = degToRad(sample(rightArmX, t, 0));

		model.body.xRot = bx;
		model.head.xRot = hx;
		model.leftArm.xRot = lax;
		model.leftArm.yRot = lay;
		model.leftArm.zRot = laz;
		model.rightArm.xRot = rax;
	}
} 