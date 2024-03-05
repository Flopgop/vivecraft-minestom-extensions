package net.flamgop.vme.listeners;


import net.flamgop.vme.VME;
import net.flamgop.vme.VivePlayer;
import net.flamgop.vme.util.MetadataHelper;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class VivecraftNetworkListener {
    private final VME vme;

    public VivecraftNetworkListener(VME vme){
        this.vme = vme;
    }

    public enum PacketDiscriminators {
        VERSION,
        REQUESTDATA,
        HEADDATA,
        CONTROLLER0DATA,
        CONTROLLER1DATA,
        WORLDSCALE,
        DRAW,
        MOVEMODE,
        UBERPACKET,
        TELEPORT,
        CLIMBING,
        SETTING_OVERRIDE,
        HEIGHT,
        ACTIVEHAND,
        CRAWL,
        NETWORK_VERSION,
        VR_SWITCHING,
        IS_VR_ACTIVE,
        VR_PLAYER_STATE
    }

    public void onPluginMessageReceived(String channel, Player sender, byte[] payload) {

        if(!channel.equalsIgnoreCase(VME.CHANNEL)) return;

        if(payload.length==0) return;

        VivePlayer vp = VME.getVivePlayers().get(sender.getUuid());

        PacketDiscriminators disc = PacketDiscriminators.values()[payload[0]];
        if(vp == null && disc != PacketDiscriminators.VERSION) {
            //how?
            return;
        }

        byte[] data = Arrays.copyOfRange(payload, 1, payload.length);
        switch (disc) {
            case CONTROLLER0DATA:
                vp.controller0data = data;
                MetadataHelper.updateMetadata(vp);
                break;
            case CONTROLLER1DATA:
                vp.controller1data = data;
                MetadataHelper.updateMetadata(vp);
                break;
            case DRAW:
                vp.draw = data;
                break;
            case HEADDATA:
                vp.hmdData = data;
                MetadataHelper.updateMetadata(vp);
                break;
            case REQUESTDATA:
                //only we can use that word.
                break;
            case VERSION:
                vp = new VivePlayer(sender);
                ByteArrayInputStream byin = new ByteArrayInputStream(data);
                DataInputStream da = new DataInputStream(byin);
                InputStreamReader is = new InputStreamReader(da);
                BufferedReader br = new BufferedReader(is);
                VME.getVivePlayers().put(sender.getUuid(), vp);

                sender.sendPluginMessage(VME.CHANNEL, StringToPayload(PacketDiscriminators.VERSION, "Vivecraft-Spigot-Extensions")); // whatever man

                try {
                    String version = br.readLine();
                    vp.version = version;
                    vp.setVR(!version.contains("NONVR"));

                    if(vme.getConfig().sendPlayerDataEnabled())
                        sender.sendPluginMessage(VME.CHANNEL, new byte[]{(byte) PacketDiscriminators.REQUESTDATA.ordinal()});

                    if(vme.getConfig().crawlingEnabled())
                        sender.sendPluginMessage(VME.CHANNEL, new byte[]{(byte) PacketDiscriminators.CRAWL.ordinal()});

                    sender.sendPluginMessage(VME.CHANNEL, new byte[]{(byte) PacketDiscriminators.VR_SWITCHING.ordinal(), 0});

                    if(vme.getConfig().climbeyEnabled()){

                        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                        byteArrayOutputStream.write(PacketDiscriminators.CLIMBING.ordinal());
                        byteArrayOutputStream.write(1); // climbey allowed

                        String mode = vme.getConfig().climbeyBlockMode();
                        if(!sender.hasPermission(vme.getConfig().climbeyPermission())){
                            if(mode.trim().equalsIgnoreCase("include"))
                                byteArrayOutputStream.write(1);
                            else if(mode.trim().equalsIgnoreCase("exclude"))
                                byteArrayOutputStream.write(2);
                            else
                                byteArrayOutputStream.write(0);
                        } else {
                            byteArrayOutputStream.write(0);
                        }

                        for (String block : vme.blockList()) {
                            if (!writeString(byteArrayOutputStream, block))
                                System.out.println("Vivecraft Minestom Extensions [WARNING]: Block name too long: " + block);
                        }

                        final byte[] p = byteArrayOutputStream.toByteArray();
                        sender.sendPluginMessage(VME.CHANNEL, p);
                    }

                    if (vme.getConfig().teleportLimitedSurvival()) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        baos.write(PacketDiscriminators.SETTING_OVERRIDE.ordinal());

                        writeSetting(baos, "limitedTeleport", true); // do it
                        writeSetting(baos, "teleportLimitUp", Math.clamp(vme.getConfig().upLimit(), 0, 4));
                        writeSetting(baos, "teleportLimitDown", Math.clamp(vme.getConfig().downLimit(), 0, 16));
                        writeSetting(baos, "teleportLimitHoriz", Math.clamp(vme.getConfig().horizontalLimit(), 0, 32));

                        final byte[] p = baos.toByteArray();
                        sender.sendPluginMessage(VME.CHANNEL, p);
                    }

                    if (vme.getConfig().worldScaleLimitRange()) {
                        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        baos.write(PacketDiscriminators.SETTING_OVERRIDE.ordinal());

                        writeSetting(baos, "worldScale.min", Math.clamp(vme.getConfig().worldScaleMin(), 0.1, 100));
                        writeSetting(baos, "worldScale.max", Math.clamp(vme.getConfig().worldScaleMax(), 0.1, 100));

                        final byte[] p = baos.toByteArray();
                        sender.sendPluginMessage(VME.CHANNEL, p);
                    }

                    if (vme.getConfig().teleportEnabled())
                        sender.sendPluginMessage(VME.CHANNEL, new byte[]{(byte) PacketDiscriminators.TELEPORT.ordinal()});

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case WORLDSCALE:
                ByteArrayInputStream a = new ByteArrayInputStream(data);
                DataInputStream b = new DataInputStream(a);
                try {
                    vp.worldScale = b.readFloat();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case HEIGHT:
                ByteArrayInputStream a1 = new ByteArrayInputStream(data);
                DataInputStream b1 = new DataInputStream(a1);
                try {
                    vp.heightScale(b1.readFloat());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                break;
            case TELEPORT:
                if (!vme.getConfig().teleportEnabled())
                    break;

                ByteArrayInputStream in = new ByteArrayInputStream(data);
                DataInputStream d = new DataInputStream(in);
                try {
                    float x = d.readFloat();
                    float y = d.readFloat();
                    float z = d.readFloat();
                    Pos newPos = new Pos(x,y,z, sender.getPosition().yaw(), sender.getPosition().pitch());
                    sender.refreshPosition(newPos);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            case CLIMBING:
                sender.refreshOnGround(true);
                break;
            case ACTIVEHAND:
                ByteArrayInputStream a2 = new ByteArrayInputStream(data);
                DataInputStream b2 = new DataInputStream(a2);
                try {
                    vp.activeHand(b2.readByte());
                    if (vp.isSeated()) vp.activeHand((byte) 0x00);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case CRAWL:
                if (!vme.getConfig().crawlingEnabled())
                    break;
                ByteArrayInputStream a3 = new ByteArrayInputStream(data);
                DataInputStream b3 = new DataInputStream(a3);
                try {
                    vp.crawling = b3.readBoolean();
                    if (vp.crawling)
                        sender.setPose(Entity.Pose.SWIMMING);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                break;
            case IS_VR_ACTIVE:
                ByteArrayInputStream vrb = new ByteArrayInputStream(data);
                DataInputStream vrd = new DataInputStream(vrb);
                boolean vr;
                try {
                    vr = vrd.readBoolean();
                    if(vp.isVR()==vr) break;
                    vp.setVR(vr);
                    if (!vr) {
                        vme.sendVRActiveUpdate(vp);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case NETWORK_VERSION:
                //don't care yet.
                break;
            case VR_PLAYER_STATE:
                //todo.
                break;
            default:
                break;
        }
    }

    public void writeSetting(ByteArrayOutputStream output, String name, Object value) {
        if (!writeString(output, name)) {
            System.out.println("Vivecraft Minestom Extensions [WARNING]: Setting name too long: " + name);
            return;
        }
        if (!writeString(output, value.toString())) {
            System.out.println("Vivecraft Minestom Extensions [WARNING]: Setting value too long: " + value);
            writeString(output, "");
        }
    }

    public static byte[] StringToPayload(PacketDiscriminators version, String input){
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        output.write((byte)version.ordinal());
        if(!writeString(output, input)) {
            output.reset();
            return output.toByteArray();
        }

        return output.toByteArray();

    }

    public static boolean writeString(ByteArrayOutputStream output, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        int len = bytes.length;
        try {
            if(!writeVarInt(output, len, 2))
                return false;
            output.write(bytes);
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static int varIntByteCount(int toCount)
    {
        return (toCount & 0xFFFFFF80) == 0 ? 1 : ((toCount & 0xFFFFC000) == 0 ? 2 : ((toCount & 0xFFE00000) == 0 ? 3 : ((toCount & 0xF0000000) == 0 ? 4 : 5)));
    }

    public static boolean writeVarInt(ByteArrayOutputStream to, int toWrite, int maxSize)
    {
        if (varIntByteCount(toWrite) > maxSize) return false;
        while ((toWrite & -128) != 0)
        {
            to.write(toWrite & 127 | 128);
            toWrite >>>= 7;
        }

        to.write(toWrite);
        return true;
    }
}
