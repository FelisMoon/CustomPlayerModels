package com.tom.cpmoscc;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tom.cpl.math.MathHelper;
import com.tom.cpm.shared.animation.AnimationType;
import com.tom.cpm.shared.animation.CustomPose;
import com.tom.cpm.shared.animation.Gesture;
import com.tom.cpmoscc.gui.OSCDataPanel.OSCChannel;

public class OSCMapping {
	private static final Pattern PATH_PARSER = Pattern.compile("osc:([\\w/]+)(?:\\[?(\\d)*:?(\\w*)\\])?(?:\\(([-\\.\\,\\d]+):([-\\.\\,\\d]+)\\))?");

	private final String animationId;
	private String oscPacketId;
	private String argMatcher;
	private int argumentId;
	private float min, max;
	private boolean boolOnly;

	private int previousValue;
	private int currentValue;

	public OSCMapping(CustomPose pose) {
		this.animationId = pose.getName();
		this.boolOnly = true;
		parseName();
	}

	public OSCMapping(Gesture gesture) {
		this.animationId = gesture.getName();
		this.boolOnly = gesture.type != AnimationType.VALUE_LAYER;
		parseName();
	}

	public OSCMapping(String displayName) {
		this.animationId = displayName;
		parseName();
	}

	private void parseName() {
		Matcher m = PATH_PARSER.matcher(animationId);
		try {
			if(m.matches()) {
				String v = m.group(2);
				argumentId = v == null || v.isEmpty() ? 0 : Integer.parseInt(v);
				argMatcher = m.group(3);
				if(argMatcher != null && argumentId == 0)argumentId++;
				v = m.group(4);
				min = v == null || v.isEmpty() ? 0 : Float.parseFloat(v.replace(',', '.'));
				v = m.group(5);
				max = v == null || v.isEmpty() ? 0 : Float.parseFloat(v.replace(',', '.'));
				oscPacketId = m.group(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getOscPacketId() {
		return oscPacketId;
	}

	@Override
	public String toString() {
		return String.format(
				"OSCMapping [animationId=%s, oscPacketId=%s, argMatcher=%s, argumentId=%s, min=%s, max=%s, boolOnly=%s]",
				animationId, oscPacketId, argMatcher, argumentId, min, max, boolOnly);
	}

	public void applyOsc(List<Object> args) {
		if(args != null && args.size() > argumentId && (argMatcher == null || argMatcher.equals(args.get(0)))) {
			Object arg = args.get(argumentId);
			int newVal = -1;
			if(arg instanceof Boolean) {
				boolean b = (boolean) arg;
				newVal = b ? 255 : 0;
			} else if(arg instanceof Integer) {
				int a = (int) arg;
				if(min == max)newVal = MathHelper.clamp(a, 0, 255);
				else {
					float v = (a - min) / (max - min);
					newVal = (int) (MathHelper.clamp(v, 0, 1) * 0xff);
				}
			} else if(arg instanceof Number) {
				float a = ((Number) arg).floatValue();
				if(min == max)newVal = (int) (MathHelper.clamp(a, 0, 1) * 0xff);
				else {
					float v = (a - min) / (max - min);
					newVal =  (int) (MathHelper.clamp(v, 0, 1) * 0xff);
				}
			}
			if(boolOnly)currentValue = newVal > 127 ? 1 : 0;
			else currentValue = newVal;
		}
	}

	public void tick() {
		if(currentValue != previousValue) {
			previousValue = currentValue;
			CPMOSC.api.playAnimation(animationId, currentValue);
		}
	}

	public OSCChannel toChannel() {
		return new OSCChannel(oscPacketId, argMatcher, argumentId, min, max);
	}
}