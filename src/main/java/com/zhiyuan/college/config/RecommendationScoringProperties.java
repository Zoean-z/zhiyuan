package com.zhiyuan.college.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "recommendation.scoring")
public class RecommendationScoringProperties {

    private int minimumProbability = 35;
    private double rankWeight = 0.75;
    private double scoreWeight = 0.25;
    private final StrategyBand rush = new StrategyBand(35, 54);
    private final StrategyBand safe = new StrategyBand(55, 74);
    private final StrategyBand guarantee = new StrategyBand(75, 100);

    public int getMinimumProbability() {
        return minimumProbability;
    }

    public void setMinimumProbability(int minimumProbability) {
        this.minimumProbability = minimumProbability;
    }

    public double getRankWeight() {
        return rankWeight;
    }

    public void setRankWeight(double rankWeight) {
        this.rankWeight = rankWeight;
    }

    public double getScoreWeight() {
        return scoreWeight;
    }

    public void setScoreWeight(double scoreWeight) {
        this.scoreWeight = scoreWeight;
    }

    public StrategyBand getRush() {
        return rush;
    }

    public StrategyBand getSafe() {
        return safe;
    }

    public StrategyBand getGuarantee() {
        return guarantee;
    }

    public static class StrategyBand {

        private int min;
        private int max;

        public StrategyBand() {
        }

        public StrategyBand(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public void setMin(int min) {
            this.min = min;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }
    }
}
