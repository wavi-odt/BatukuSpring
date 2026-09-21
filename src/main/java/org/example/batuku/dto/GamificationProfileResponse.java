package org.example.batuku.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GamificationProfileResponse {

    private int    totalPoints;
    private int    level;
    private int    experiencePoints;
    private int    pointsToNextLevel;
    private long   rank;
    private List<BadgeDto> badges;

    public static class BadgeDto {
        private Long          id;
        private String        name;
        private String        description;
        private String        iconUrl;
        private int           pointsRequired;
        private LocalDateTime earnedAt;

        public Long          getId()             { return id;             }
        public String        getName()           { return name;           }
        public String        getDescription()    { return description;    }
        public String        getIconUrl()        { return iconUrl;        }
        public int           getPointsRequired() { return pointsRequired; }
        public LocalDateTime getEarnedAt()       { return earnedAt;       }

        public void setId(Long id)                       { this.id = id;                       }
        public void setName(String name)                 { this.name = name;                   }
        public void setDescription(String description)   { this.description = description;     }
        public void setIconUrl(String iconUrl)           { this.iconUrl = iconUrl;             }
        public void setPointsRequired(int p)             { this.pointsRequired = p;            }
        public void setEarnedAt(LocalDateTime earnedAt)  { this.earnedAt = earnedAt;           }
    }

    public int              getTotalPoints()      { return totalPoints;      }
    public int              getLevel()            { return level;            }
    public int              getExperiencePoints() { return experiencePoints; }
    public int              getPointsToNextLevel(){ return pointsToNextLevel;}
    public long             getRank()             { return rank;             }
    public List<BadgeDto>   getBadges()           { return badges;           }

    public void setTotalPoints(int totalPoints)           { this.totalPoints = totalPoints;           }
    public void setLevel(int level)                       { this.level = level;                       }
    public void setExperiencePoints(int xp)               { this.experiencePoints = xp;               }
    public void setPointsToNextLevel(int p)               { this.pointsToNextLevel = p;               }
    public void setRank(long rank)                        { this.rank = rank;                         }
    public void setBadges(List<BadgeDto> badges)          { this.badges = badges;                     }
}
