package org.example.batuku.dto;

public class ChallengeResponse {

    private String  id;
    private String  icon;
    private String  title;
    private String  desc;
    private int     progress;
    private int     total;
    private int     xp;
    private String  expires;
    private boolean completed;
    private int     setIndex;
    private int     totalSets;

    public String  getId()        { return id;        }
    public String  getIcon()      { return icon;      }
    public String  getTitle()     { return title;     }
    public String  getDesc()      { return desc;      }
    public int     getProgress()  { return progress;  }
    public int     getTotal()     { return total;     }
    public int     getXp()        { return xp;        }
    public String  getExpires()   { return expires;   }
    public boolean isCompleted()  { return completed; }
    public int     getSetIndex()  { return setIndex;  }
    public int     getTotalSets() { return totalSets; }

    public void setId(String id)             { this.id = id;             }
    public void setIcon(String icon)         { this.icon = icon;         }
    public void setTitle(String title)       { this.title = title;       }
    public void setDesc(String desc)         { this.desc = desc;         }
    public void setProgress(int progress)    { this.progress = progress; }
    public void setTotal(int total)          { this.total = total;       }
    public void setXp(int xp)               { this.xp = xp;             }
    public void setExpires(String expires)   { this.expires = expires;   }
    public void setCompleted(boolean c)      { this.completed = c;       }
    public void setSetIndex(int setIndex)    { this.setIndex = setIndex; }
    public void setTotalSets(int totalSets)  { this.totalSets = totalSets; }
}
