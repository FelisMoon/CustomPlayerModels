package com.tom.cpm.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.util.math.Matrix4f;

import com.tom.cpm.client.PlayerRenderManager.Mat4Access;

@Mixin(Matrix4f.class)
public class Matrix4fMixin implements Mat4Access {
	@Shadow protected float a00;
	@Shadow protected float a01;
	@Shadow protected float a02;
	@Shadow protected float a03;
	@Shadow protected float a10;
	@Shadow protected float a11;
	@Shadow protected float a12;
	@Shadow protected float a13;
	@Shadow protected float a20;
	@Shadow protected float a21;
	@Shadow protected float a22;
	@Shadow protected float a23;
	@Shadow protected float a30;
	@Shadow protected float a31;
	@Shadow protected float a32;
	@Shadow protected float a33;

	@Override
	public void cpm$loadValue(float[] values) {
		a00 = values[0];
		a01 = values[1];
		a02 = values[2];
		a03 = values[3];
		a10 = values[4];
		a11 = values[5];
		a12 = values[6];
		a13 = values[7];
		a20 = values[8];
		a21 = values[9];
		a22 = values[10];
		a23 = values[11];
		a30 = values[12];
		a31 = values[13];
		a32 = values[14];
		a33 = values[15];
	}
}
