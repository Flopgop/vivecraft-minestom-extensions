package net.flamgop.vme.math;

import net.minestom.server.coordinate.Vec;

public class MathUtil {

    public static Vec mul(Vec vec, Quaternion quat) {
        Quaternion q = quat.multiply(new Quaternion(vec));
        return new Vec(q.x(), q.y(), q.z());
    }
}
