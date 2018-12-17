/* Lakelis
 * 
 * Victoria Road: Kerning City (103000000)
 * 
 * Kerning City Party Quest NPC 
*/

var status = 0;
var minLevel = 1;
var maxLevel = 255;
var minPlayers = 0;
var maxPlayers = 6;

function start() {
	status = -1;
	action(1, 0, 0);
}

function action(mode, type, selection) {
	if (mode == -1) {
		cm.dispose();
	} else {
		if (mode == 0 && status == 0) {
			cm.dispose();
			return;
		}
		if (mode == 1)
			status++;
		else
			status--;
		if (status == 0) {
			// Lakelis has no preamble, directly checks if you're in a party
			if (cm.getParty() == null) { // no party
				cm.sendOk("How about you and your party members collectively beating a quest? Here you'll find obstacles and problems where you won't be able to beat it without great teamwork.  If you want to try it, please tell the #bleader of your party#k to talk to me.");
				cm.dispose();
                                return;
			}
			if (!cm.isLeader()) { // not party leader
				cm.sendSimple("If you want to try the quest, please tell the #bleader of your party#k to talk to me.");
				cm.dispose();
                        }
			else {
				// check if all party members are within 21-30 range, etc.
				var party = cm.getParty().getMembers();
				var mapId = cm.getChar().getMapId();
				var next = true;
				var levelValid = 0;
				var inMap = 0;
				// Temp removal for testing
				var it = party.iterator();
				while (it.hasNext()) {
					var cPlayer = it.next();
					if ((cPlayer.getLevel() >= minLevel) && (cPlayer.getLevel() <= maxLevel)) {
						levelValid += 1;
					}
					if (cPlayer.getMapid() == mapId) {
						inMap += 1;
					}
				}
				if (party.size() < minPlayers || party.size() > maxPlayers) 
					next = false;
				else if (levelValid < minPlayers || inMap < minPlayers) {
						next = false;
				}
				if (next) {
					// Kick it into action.  Lakelis says nothing here, just warps you in.
					var em = cm.getEventManager("KerningPQ");
					if (em == null) {
						cm.sendOk("This PQ is not currently available.");
					}
					else {
						// Begin the PQ.
						em.startInstance(cm.getParty(),cm.getChar().getMap());
						// Remove pass/coupons
						party = cm.getChar().getEventInstance().getPlayers();
						cm.removeFromParty(4001008, party);
						cm.removeFromParty(4001007, party);
					}
					cm.dispose();
				}
				else {
					cm.sendOk("Your party is not a party of four. Please make sure all your members are present and qualified to participate in this quest.  I see #b" + levelValid.toString() + " #kmembers are in the right level range, and #b" + inMap.toString() + "#k are in Kerning. If this seems wrong, #blog out and log back in,#k or reform the party.");
					cm.dispose();
				}
			}
		}
		else {
			cm.sendOk("Dialog is broken.");
			cm.dispose();
		}
	}
}
					