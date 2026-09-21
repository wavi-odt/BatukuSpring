package org.example.batuku.dto;

import java.util.List;

public class LeaderboardResponse {

    private List<EntryDto> entries;

    public static class EntryDto {
        private long   rank;
        private Long   userId;
        private String username;
        private String name;
        private String avatarUrl;
        private int    totalPoints;
        private int    level;
        private int    badgeCount;

        public long   getRank()        { return rank;        }
        public Long   getUserId()      { return userId;      }
        public String getUsername()    { return username;    }
        public String getName()        { return name;        }
        public String getAvatarUrl()   { return avatarUrl;   }
        public int    getTotalPoints() { return totalPoints; }
        public int    getLevel()       { return level;       }
        public int    getBadgeCount()  { return badgeCount;  }

        public void setRank(long rank)             { this.rank = rank;               }
        public void setUserId(Long userId)         { this.userId = userId;           }
        public void setUsername(String username)   { this.username = username;       }
        public void setName(String name)           { this.name = name;               }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl;     }
        public void setTotalPoints(int p)          { this.totalPoints = p;           }
        public void setLevel(int level)            { this.level = level;             }
        public void setBadgeCount(int badgeCount)  { this.badgeCount = badgeCount;   }
    }

    public List<EntryDto> getEntries() { return entries; }
    public void setEntries(List<EntryDto> entries) { this.entries = entries; }
}
