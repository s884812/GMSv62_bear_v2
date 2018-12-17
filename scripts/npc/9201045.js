var status = 0; 
var maps = Array(100000005, 105070002, 800010100, 105090900, 682000001, 240020101, 240020401, 230040420, 990000900, 801040003, 220080001, 211042400, 240060200, 800020130); 
var mapsname = Array("鐵甲豬公園Ⅲ(蘑菇王)", "蘑菇王之墓(殭屍蘑菇王)", "天皇殿堂(藍蘑菇王)", "被詛咒的寺院(蝙蝠怪)", "幻影森林(魔馬)", "格瑞芬多森林(天鷹)", "噴火龍棲息地(肥龍)", "皮亞奴斯洞穴(魚王)", "艾裡斯王座", "日本御姐", "時間塔的本源(鬧鐘)", "炎魔", "暗黑龍王洞穴", "天狗"); 
var selectedMap = -1; 

function start() { 
    status = -1; 
    action(1, 0, 0); 
} 

function action(mode, type, selection) { 
    if (mode == -1) { 
        cm.dispose(); 
    } else { 
        if (status >= 3 && mode == 0) { 
            cm.sendOk("好的,如果你決定要去哪裡,我會很樂意傳送你的."); 
            cm.dispose(); 
            return; 
        } 
        if (mode == 1) 
            status++; 
        else { 
            cm.sendOk("好的,如果你決定要去哪裡,我會很樂意傳送你的."); 
            cm.dispose(); 
            return; 
        } if (status == 0) { 
            cm.sendYesNo("HI，我是VIP專用BOSS級的怪物傳送員，聽說GM正在考慮取消普通玩家的部分BOSS傳送，不過你是VIP的話就不用擔心了！"); 
        } else if (status == 1) { 
            var selStr = "選擇你的目的地.#b"; 
                for (var i = 0; i < maps.length; i++) { 
                selStr += "\r\n#L" + i + "#" + mapsname[ i ]+""; 
                } 
            cm.sendSimple(selStr); 
            
        } else if (status == 2) { 
            cm.sendYesNo("你真的要去 " + mapsname[selection] + "嗎?"); 
            selectedMap = selection; 
        } 
        
        else if (status == 3) { 
            cm.warp(maps[selectedMap], 0); 
            cm.dispose(); 
        }
    }
}