package net.flamgop.vme.util;

import net.flamgop.vme.VivePlayer;
import net.flamgop.vme.math.Quaternion;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jglrxavpok.hephaistos.nbt.NBT;

import java.util.Map;

public class MetadataHelper {
    public static void updateMetadata(final VivePlayer data) {
        setPosTag(data.player, "head.pos", getPos(data.getHMDPos(), data.getHMDDir()));
        setDirTag(data.player, "head.aim", data.getHMDDir()); // God forbid someone is using this...
        setDirTag(data.player, "head.dir", getAim(data.getHMDDir()));
        setQuatTag(data.player, "head.rot", data.getHMDRot());

        setPosTag(data.player, "righthand.pos", getPos(data.getControllerPos(0), data.getControllerDir(0)));
        setDirTag(data.player, "righthand.aim", data.getControllerDir(0)); // Really seriously don't use this one.
        setDirTag(data.player, "righthand.dir", getAim(data.getControllerDir(0)));
        setQuatTag(data.player, "righthand.rot", data.getControllerRot(0));

        setPosTag(data.player, "lefthand.pos", getPos(data.getControllerPos(1), data.getControllerDir(1)));
        setDirTag(data.player, "lefthand.aim", data.getControllerDir(1)); // It's an nms class, don't use it, use the other one.
        setDirTag(data.player, "lefthand.dir", getAim(data.getControllerDir(1)));
        setQuatTag(data.player, "lefthand.rot", data.getControllerRot(1));

        data.player.setTag(Tag.Boolean("seated"), data.isSeated());
        data.player.setTag(Tag.Float("height"), data.heightScale());
        data.player.setTag(Tag.String("activatehand"), data.activeHand() == 0 ? "right" : "left");
    }

    public static void setQuatTag(Player player, String key, Quaternion quat) {
        player.setTag(Tag.NBT(key), NBT.Compound(Map.of(
                "qx", NBT.Double(quat.x()),
                "qy", NBT.Double(quat.y()),
                "qz", NBT.Double(quat.z()),
                "qw", NBT.Double(quat.w())
        )));
    }

    public static void setDirTag(Player player, String key, Vec dir) {
        player.setTag(Tag.NBT(key), NBT.Compound(Map.of(
                "rx", NBT.Double(dir.x()),
                "ry", NBT.Double(dir.y()),
                "rz", NBT.Double(dir.z())
        )));
    }

    public static void setPosTag(Player player, String key, Pos pos) {
        player.setTag(Tag.NBT(key), NBT.Compound(Map.of(
                        "x", NBT.Double(pos.x()),
                        "y", NBT.Double(pos.y()),
                        "z", NBT.Double(pos.z()),
                        "rx", NBT.Double(pos.direction().x()),
                        "ry", NBT.Double(pos.direction().y()),
                        "rz", NBT.Double(pos.direction().z())
                )));
    }

    public static void cleanupMetadata(Player player) {
        player.removeTag(Tag.NBT("head.pos"));
        player.removeTag(Tag.NBT("head.aim"));
        player.removeTag(Tag.NBT("head.dir"));
        player.removeTag(Tag.NBT("head.rot"));

        player.removeTag(Tag.NBT("righthand.pos"));
        player.removeTag(Tag.NBT("righthand.aim"));
        player.removeTag(Tag.NBT("righthand.dir"));
        player.removeTag(Tag.NBT("righthand.rot"));

        player.removeTag(Tag.NBT("lefthand.pos"));
        player.removeTag(Tag.NBT("lefthand.aim"));
        player.removeTag(Tag.NBT("lefthand.dir"));
        player.removeTag(Tag.NBT("lefthand.rot"));

        player.removeTag(Tag.Boolean("seated"));
        player.removeTag(Tag.Float("height"));
        player.removeTag(Tag.String("activehand"));
    }
    private static Pos getPos(Pos pos, Vec dir) {
        return pos.withDirection(getAim(dir));
    }

    private static Vec getAim(Vec dir) {
        return new Vec(dir.x(), dir.y(), dir.z());
    }
}