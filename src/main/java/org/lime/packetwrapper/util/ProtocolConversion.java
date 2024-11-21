package com.comphenix.packetwrapper.util;

/**
 * @author Lukas Alt
 * @since 15.05.2023
 */
public class ProtocolConversion {
    public static float angleToDegrees(byte rawAngle) {
        return rawAngle / 256.0F * 360.0F;
    }

    public static byte degreesToAngle(float degree) {
        return (byte)((int)(degree * 256.0F / 360.0F));
    }
}
