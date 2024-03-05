package net.flamgop.vme.math;

import net.minestom.server.coordinate.Vec;

public record Quaternion(double x, double y, double z, double w) {

    public Quaternion(Vec vec) {
        this(vec.x(), vec.y(), vec.z(), 0);
    }

    public Quaternion() {
        this(0,0,0,1);
    }

    Quaternion multiply(Quaternion q) {
        double newX = w * q.x() + x * q.w() + y * q.z() - z * q.y();
        double newY = w * q.y() - x * q.z() + y * q.w() + z * q.x();
        double newZ = w * q.z() + x * q.y() - y * q.x() + z * q.w();
        double newW = w * q.w() - x * q.x() - y * q.y() - z * q.z();
        return new Quaternion(newX, newY, newZ, newW);
    }
}
