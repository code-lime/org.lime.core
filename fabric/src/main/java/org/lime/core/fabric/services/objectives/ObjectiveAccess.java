package org.lime.core.fabric.services.objectives;

import net.kyori.adventure.text.Component;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

public record ObjectiveAccess(
        ObjectiveService owner,
        String objectiveName) {
    public Objective handle() {
        var objective = owner.scoreboard.getObjective(objectiveName);
        if (objective != null)
            return objective;
        return owner.scoreboard.addObjective(
                objectiveName,
                ObjectiveCriteria.DUMMY,
                owner.nativeComponent.convert(Component.text(objectiveName)),
                ObjectiveCriteria.RenderType.INTEGER
                //#switch PROPERTIES.versionMinecraft
                //#caseofregex 1\.21\.*
                //OF//                , false, null
                //#default
                //#endswitch
        );
    }

    //#switch PROPERTIES.versionMinecraft
    //#caseofregex 1\.21\.*
    //OF//    public ScoreHolder scoreKey(UUID player, String key) {
    //OF//        return ScoreHolder.forNameOnly(player + "." + key);
    //OF//    }
    //OF//    public ScoreHolder scoreKey(String key) {
    //OF//        return ScoreHolder.forNameOnly(key);
    //OF//    }
    //OF//    public ScoreAccess getOrCreateScore(ScoreHolder scoreKey) {
    //OF//        return owner.scoreboard.getOrCreatePlayerScore(scoreKey, handle());
    //OF//    }
    //OF//    public ScoreAccess getOrCreateScore(PlayerScoreEntry entry) {
    //OF//        return getOrCreateScore(ScoreHolder.forNameOnly(entry.owner()));
    //OF//    }
    //OF//    public int getValue(ScoreAccess score) {
    //OF//        return score.get();
    //OF//    }
    //OF//    public int getValue(PlayerScoreEntry entry) {
    //OF//        return getValue(getOrCreateScore(entry));
    //OF//    }
    //OF//    public void addValue(ScoreAccess score, int value) {
    //OF//        score.add(value);
    //OF//    }
    //OF//    public void addValue(PlayerScoreEntry entry, int value) {
    //OF//        addValue(getOrCreateScore(entry), value);
    //OF//    }
    //OF//    public void setValue(ScoreAccess score, @Nullable Integer value) {
    //OF//        if (value == null) score.reset();
    //OF//        else score.set(value);
    //OF//    }
    //OF//    public void setValue(PlayerScoreEntry entry, @Nullable Integer value) {
    //OF//        setValue(getOrCreateScore(entry), value);
    //OF//    }
    //OF//    public Collection<PlayerScoreEntry> scores() {
    //OF//        return owner.scoreboard.listPlayerScores(handle());
    //OF//    }
    //OF//    public ScoreProvider<ScoreHolder> provider(ScoreHolder key) {
    //OF//        return new Impl(key, this);
    //OF//    }
    //OF//    public Stream<ScoreProvider<ScoreHolder>> providers() {
    //OF//        return scores().stream().map(PlayerScoreEntry::owner).map(this::scoreKey).map(this::provider);
    //OF//    }
    //#default
    public String scoreKey(UUID player, String key) {
        return player + "." + key;
    }
    public String scoreKey(String key) {
        return key;
    }
    public Score getOrCreateScore(String scoreKey) {
        return owner.scoreboard.getOrCreatePlayerScore(scoreKey, handle());
    }
    public int getValue(Score score) {
        return score.getScore();
    }
    public void addValue(Score score, int value) {
        score.add(value);
    }
    public void setValue(Score score, @Nullable Integer value) {
        if (value == null) score.reset();
        else score.setScore(value);
    }
    public Collection<Score> scores() {
        return owner.scoreboard.getPlayerScores(handle());
    }
    public ScoreProvider<String> provider(String key) {
        return new Impl(key, this);
    }
    public Stream<ScoreProvider<String>> providers() {
        return scores().stream().map(Score::getOwner).map(this::provider);
    }
    //#endswitch

    //#switch PROPERTIES.versionMinecraft
    //#caseofregex 1\.21\.*
    //OF//    private static class Impl
    //OF//            extends ScoreProvider<ScoreHolder> {
    //OF//        public Impl(ScoreHolder s, ObjectiveAccess owner) {
    //OF//            super(s, owner);
    //OF//        }
    //#default
    private static class Impl
            extends ScoreProvider<String> {
        public Impl(String s, ObjectiveAccess owner) {
            super(s, owner);
        }
    //#endswitch
        @Override
        public int get() {
            return owner.getValue(owner.getOrCreateScore(key));
        }
        @Override
        public void set(int value) {
            owner.setValue(owner.getOrCreateScore(key), value);
        }
        @Override
        public int add(int value) {
            var score = owner.getOrCreateScore(key);
            owner.setValue(score, value);
            return owner.getValue(score);
        }
        @Override
        public void reset() {
            owner.getOrCreateScore(key).reset();
        }
    }
}
