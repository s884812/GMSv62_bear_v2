package net.sf.odinms.net.channel.handler;

import java.rmi.RemoteException;

import net.sf.odinms.client.*;
import net.sf.odinms.net.AbstractMaplePacketHandler;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.remote.WorldChannelInterface;
import net.sf.odinms.server.MTSItemInfo;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.data.input.SeekableLittleEndianAccessor;
import net.sf.odinms.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import net.sf.odinms.server.maps.SavedLocationType;

public class EnterMTSHandler extends AbstractMaplePacketHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DistributeSPHandler.class);

    @Override
    public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        if (c.getChannelServer().allowMTS()) {
            if (c.getPlayer().getNoPets() > 0) {
                c.getPlayer().unequipAllPets();
            }
            try {
                WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
                wci.addBuffsToStorage(c.getPlayer().getId(), c.getPlayer().getAllBuffs());
                wci.addCooldownsToStorage(c.getPlayer().getId(), c.getPlayer().getAllCooldowns());
            } catch (RemoteException e) {
                c.getChannelServer().reconnectWorld();
            }
            c.getPlayer().getMap().removePlayer(c.getPlayer());
            c.getSession().write(MaplePacketCreator.warpMTS(c));
            c.getPlayer().setInMTS(true);
            c.getSession().write(MaplePacketCreator.enableMTS());
            c.getSession().write(MaplePacketCreator.MTSWantedListingOver(0, 0));
            c.getSession().write(MaplePacketCreator.showMTSCash(c.getPlayer()));
            List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
            int pages = 0;
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = 1 AND transfer = 0 ORDER BY id DESC LIMIT ?, 16");
                ps.setInt(1, 0);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getInt("type") != 1) {
                        Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                        i.setOwner(rs.getString("owner"));
                        items.add(new MTSItemInfo(i, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                    } else {
                        Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                        equip.setOwner(rs.getString("owner"));
                        equip.setQuantity((short) 1);
                        equip.setAcc((short) rs.getInt("acc"));
                        equip.setAvoid((short) rs.getInt("avoid"));
                        equip.setDex((short) rs.getInt("dex"));
                        equip.setHands((short) rs.getInt("hands"));
                        equip.setHp((short) rs.getInt("hp"));
                        equip.setInt((short) rs.getInt("int"));
                        equip.setJump((short) rs.getInt("jump"));
                        equip.setLuk((short) rs.getInt("luk"));
                        equip.setMatk((short) rs.getInt("matk"));
                        equip.setMdef((short) rs.getInt("mdef"));
                        equip.setMp((short) rs.getInt("mp"));
                        equip.setSpeed((short) rs.getInt("speed"));
                        equip.setStr((short) rs.getInt("str"));
                        equip.setWatk((short) rs.getInt("watk"));
                        equip.setWdef((short) rs.getInt("wdef"));
                        equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                        equip.setLocked((byte) rs.getInt("locked"));
                        equip.setLevel((byte) rs.getInt("level"));
                        items.add(new MTSItemInfo((IItem) equip, rs.getInt("price") + 100 + (int) (rs.getInt("price") * 0.1), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                    }
                }
                rs.close();
                ps.close();
                ps = con.prepareStatement("SELECT COUNT(*) FROM mts_items");
                rs = ps.executeQuery();
                if (rs.next()) {
                    pages = (int) Math.ceil(rs.getInt(1) / 16);
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                log.error("Err1: " + e);
            }
            c.getSession().write(MaplePacketCreator.sendMTS(items, 1, 0, 0, pages));
            c.getSession().write(MaplePacketCreator.TransferInventory(getTransfer(c.getPlayer().getId())));
            c.getSession().write(MaplePacketCreator.NotYetSoldInv(getNotYetSold(c.getPlayer().getId())));
            c.getPlayer().saveToDB(true);
        } else {
            if (!(c.getPlayer().isAlive())) {
                c.getPlayer().dropMessage("You can't enter the FM when you are dead.");
                c.getSession().write(MaplePacketCreator.enableActions());
            } else {
                if (c.getPlayer().getMapId() != 910000000) {
                    c.getPlayer().dropMessage("我已經把你送到自由市場了.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                    c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET);
                    c.getPlayer().changeMap(ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(910000000), ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(910000000).getPortal("out00"));
                } else {
                    c.getPlayer().dropMessage("你已經在自由市場了不能傳送.");
                    c.getSession().write(MaplePacketCreator.enableActions());
                }
            }
        }
    }

    public List<MTSItemInfo> getNotYetSold(int cid) {
        List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        ResultSet rs;
        try {
            ps = con.prepareStatement("SELECT * FROM mts_items WHERE seller = ? AND transfer = 0 ORDER BY id DESC");
            ps.setInt(1, cid);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") != 1) {
                    Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    i.setOwner(rs.getString("owner"));
                    items.add(new MTSItemInfo((IItem) i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                } else {
                    Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                    equip.setOwner(rs.getString("owner"));
                    equip.setQuantity((short) 1);
                    equip.setAcc((short) rs.getInt("acc"));
                    equip.setAvoid((short) rs.getInt("avoid"));
                    equip.setDex((short) rs.getInt("dex"));
                    equip.setHands((short) rs.getInt("hands"));
                    equip.setHp((short) rs.getInt("hp"));
                    equip.setInt((short) rs.getInt("int"));
                    equip.setJump((short) rs.getInt("jump"));
                    equip.setLuk((short) rs.getInt("luk"));
                    equip.setMatk((short) rs.getInt("matk"));
                    equip.setMdef((short) rs.getInt("mdef"));
                    equip.setMp((short) rs.getInt("mp"));
                    equip.setSpeed((short) rs.getInt("speed"));
                    equip.setStr((short) rs.getInt("str"));
                    equip.setWatk((short) rs.getInt("watk"));
                    equip.setWdef((short) rs.getInt("wdef"));
                    equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    equip.setLocked((byte) rs.getInt("locked"));
                    equip.setLevel((byte) rs.getInt("level"));
                    items.add(new MTSItemInfo((IItem) equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            log.error("Err8: " + e);
        }
        return items;
    }

    public List<MTSItemInfo> getTransfer(int cid) {
        List<MTSItemInfo> items = new ArrayList<MTSItemInfo>();
        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps;
        ResultSet rs;
        try {
            ps = con.prepareStatement("SELECT * FROM mts_items WHERE transfer = 1 AND seller = ? ORDER BY id DESC");
            ps.setInt(1, cid);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("type") != 1) {
                    Item i = new Item(rs.getInt("itemid"), (byte) 0, (short) rs.getInt("quantity"));
                    i.setOwner(rs.getString("owner"));
                    items.add(new MTSItemInfo((IItem) i, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                } else {
                    Equip equip = new Equip(rs.getInt("itemid"), (byte) rs.getInt("position"), -1);
                    equip.setOwner(rs.getString("owner"));
                    equip.setQuantity((short) 1);
                    equip.setAcc((short) rs.getInt("acc"));
                    equip.setAvoid((short) rs.getInt("avoid"));
                    equip.setDex((short) rs.getInt("dex"));
                    equip.setHands((short) rs.getInt("hands"));
                    equip.setHp((short) rs.getInt("hp"));
                    equip.setInt((short) rs.getInt("int"));
                    equip.setJump((short) rs.getInt("jump"));
                    equip.setLuk((short) rs.getInt("luk"));
                    equip.setMatk((short) rs.getInt("matk"));
                    equip.setMdef((short) rs.getInt("mdef"));
                    equip.setMp((short) rs.getInt("mp"));
                    equip.setSpeed((short) rs.getInt("speed"));
                    equip.setStr((short) rs.getInt("str"));
                    equip.setWatk((short) rs.getInt("watk"));
                    equip.setWdef((short) rs.getInt("wdef"));
                    equip.setUpgradeSlots((byte) rs.getInt("upgradeslots"));
                    equip.setLocked((byte) rs.getInt("locked"));
                    equip.setLevel((byte) rs.getInt("level"));
                    items.add(new MTSItemInfo((IItem) equip, rs.getInt("price"), rs.getInt("id"), rs.getInt("seller"), rs.getString("sellername"), rs.getString("sell_ends")));
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            log.error("Err7: " + e);
        }
        return items;
    }
}