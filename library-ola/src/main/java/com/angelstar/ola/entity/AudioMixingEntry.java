package com.angelstar.ola.entity;

import androidx.annotation.Keep;

import java.util.List;

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/7 15:58
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 录制的歌曲调音台全量数据
 */
@Keep
public class AudioMixingEntry {

    private int songId;
    private String songName;
    private ScoreEntry score;
    private String originalFilePath;
    private String voiceFilePath;
    private String mixingFilePath;
    private int audioProfile;
    private int startTimeMs;
    private int endTimeMs;
    private int durationMs;
    private int totalDurationMs;
    private TunerModel tunerModel;
    private String effectJson;
    private List<LyricList> lyricList;
    private List<SingTimeLyricList> singTimeLyricList;
    private List<ReverbList> reverbList;
    private List<EqualizerEffects> equalizerEffects;
    private List<BoardEffects> boardEffects;
    private List<TempEffects> tempEffects;

    public int getSongId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public ScoreEntry getScore() {
        return score;
    }

    public void setScore(ScoreEntry score) {
        this.score = score;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public void setOriginalFilePath(String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

    public String getVoiceFilePath() {
        return voiceFilePath;
    }

    public void setVoiceFilePath(String voiceFilePath) {
        this.voiceFilePath = voiceFilePath;
    }

    public String getMixingFilePath() {
        return mixingFilePath;
    }

    public void setMixingFilePath(String mixingFilePath) {
        this.mixingFilePath = mixingFilePath;
    }

    public int getAudioProfile() {
        return audioProfile;
    }

    public void setAudioProfile(int audioProfile) {
        this.audioProfile = audioProfile;
    }

    public int getStartTimeMs() {
        return startTimeMs;
    }

    public void setStartTimeMs(int startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    public int getEndTimeMs() {
        return endTimeMs;
    }

    public void setEndTimeMs(int endTimeMs) {
        this.endTimeMs = endTimeMs;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public int getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(int totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public TunerModel getTunerModel() {
        return tunerModel;
    }

    public void setTunerModel(TunerModel tunerModel) {
        this.tunerModel = tunerModel;
    }

    public String getEffectJson() {
        return effectJson;
    }

    public void setEffectJson(String effectJson) {
        this.effectJson = effectJson;
    }

    public List<LyricList> getLyricList() {
        return lyricList;
    }

    public void setLyricList(List<LyricList> lyricList) {
        this.lyricList = lyricList;
    }

    public List<SingTimeLyricList> getSingTimeLyricList() {
        return singTimeLyricList;
    }

    public void setSingTimeLyricList(List<SingTimeLyricList> singTimeLyricList) {
        this.singTimeLyricList = singTimeLyricList;
    }

    public List<ReverbList> getReverbList() {
        return reverbList;
    }

    public void setReverbList(List<ReverbList> reverbList) {
        this.reverbList = reverbList;
    }

    public List<EqualizerEffects> getEqualizerEffects() {
        return equalizerEffects;
    }

    public void setEqualizerEffects(List<EqualizerEffects> equalizerEffects) {
        this.equalizerEffects = equalizerEffects;
    }

    public List<BoardEffects> getBoardEffects() {
        return boardEffects;
    }

    public void setBoardEffects(List<BoardEffects> boardEffects) {
        this.boardEffects = boardEffects;
    }

    public List<TempEffects> getTempEffects() {
        return tempEffects;
    }

    public void setTempEffects(List<TempEffects> tempEffects) {
        this.tempEffects = tempEffects;
    }

    public static class ScoreEntry {
        private String songName;
        private int perfectNum;
        private int goodNum;
        private int totalScore;
        private int avgScore;

        public String getSongName() {
            return songName;
        }

        public void setSongName(String songName) {
            this.songName = songName;
        }

        public int getPerfectNum() {
            return perfectNum;
        }

        public void setPerfectNum(int perfectNum) {
            this.perfectNum = perfectNum;
        }

        public int getGoodNum() {
            return goodNum;
        }

        public void setGoodNum(int goodNum) {
            this.goodNum = goodNum;
        }

        public int getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(int totalScore) {
            this.totalScore = totalScore;
        }

        public int getAvgScore() {
            return avgScore;
        }

        public void setAvgScore(int avgScore) {
            this.avgScore = avgScore;
        }
    }

    public static class TunerModel {
        private boolean enableInEarMonitoring;
        private boolean clickEarMonitoring;
        private boolean hasShowTuner;
        private int reverbIndex;
        private String reverbName;
        private int equalizerIndex;
        private String equalizerName;
        private int aiIndex;
        private String aiName;
        private int effectIndex;
        private int audioMixingVolume;
        private int recordSignalVolume;
        private int mixingPitch;
        private int playbackSignalVolume;
        private int earMonitoringVolume;

        public boolean isEnableInEarMonitoring() {
            return enableInEarMonitoring;
        }

        public void setEnableInEarMonitoring(boolean enableInEarMonitoring) {
            this.enableInEarMonitoring = enableInEarMonitoring;
        }

        public boolean isClickEarMonitoring() {
            return clickEarMonitoring;
        }

        public void setClickEarMonitoring(boolean clickEarMonitoring) {
            this.clickEarMonitoring = clickEarMonitoring;
        }

        public boolean isHasShowTuner() {
            return hasShowTuner;
        }

        public void setHasShowTuner(boolean hasShowTuner) {
            this.hasShowTuner = hasShowTuner;
        }

        public int getReverbIndex() {
            return reverbIndex;
        }

        public void setReverbIndex(int reverbIndex) {
            this.reverbIndex = reverbIndex;
        }

        public String getReverbName() {
            return reverbName;
        }

        public void setReverbName(String reverbName) {
            this.reverbName = reverbName;
        }

        public int getEqualizerIndex() {
            return equalizerIndex;
        }

        public void setEqualizerIndex(int equalizerIndex) {
            this.equalizerIndex = equalizerIndex;
        }

        public String getEqualizerName() {
            return equalizerName;
        }

        public void setEqualizerName(String equalizerName) {
            this.equalizerName = equalizerName;
        }

        public int getAiIndex() {
            return aiIndex;
        }

        public void setAiIndex(int aiIndex) {
            this.aiIndex = aiIndex;
        }

        public String getAiName() {
            return aiName;
        }

        public void setAiName(String aiName) {
            this.aiName = aiName;
        }

        public int getEffectIndex() {
            return effectIndex;
        }

        public void setEffectIndex(int effectIndex) {
            this.effectIndex = effectIndex;
        }

        public int getAudioMixingVolume() {
            return audioMixingVolume;
        }

        public void setAudioMixingVolume(int audioMixingVolume) {
            this.audioMixingVolume = audioMixingVolume;
        }

        public int getRecordSignalVolume() {
            return recordSignalVolume;
        }

        public void setRecordSignalVolume(int recordSignalVolume) {
            this.recordSignalVolume = recordSignalVolume;
        }

        public int getMixingPitch() {
            return mixingPitch;
        }

        public void setMixingPitch(int mixingPitch) {
            this.mixingPitch = mixingPitch;
        }

        public int getPlaybackSignalVolume() {
            return playbackSignalVolume;
        }

        public void setPlaybackSignalVolume(int playbackSignalVolume) {
            this.playbackSignalVolume = playbackSignalVolume;
        }

        public int getEarMonitoringVolume() {
            return earMonitoringVolume;
        }

        public void setEarMonitoringVolume(int earMonitoringVolume) {
            this.earMonitoringVolume = earMonitoringVolume;
        }
    }

    public static class LyricList {
        private String lyric;
        private int startTime;
        private int endTime;
        private boolean isRemark;

        public String getLyric() {
            return lyric;
        }

        public void setLyric(String lyric) {
            this.lyric = lyric;
        }

        public int getStartTime() {
            return startTime;
        }

        public void setStartTime(int startTime) {
            this.startTime = startTime;
        }

        public int getEndTime() {
            return endTime;
        }

        public void setEndTime(int endTime) {
            this.endTime = endTime;
        }

        public boolean isIsRemark() {
            return isRemark;
        }

        public void setIsRemark(boolean isRemark) {
            this.isRemark = isRemark;
        }
    }

    public static class SingTimeLyricList {
        private String lyric;
        private int startTime;
        private int endTime;
        private boolean isRemark;

        public String getLyric() {
            return lyric;
        }

        public void setLyric(String lyric) {
            this.lyric = lyric;
        }

        public int getStartTime() {
            return startTime;
        }

        public void setStartTime(int startTime) {
            this.startTime = startTime;
        }

        public int getEndTime() {
            return endTime;
        }

        public void setEndTime(int endTime) {
            this.endTime = endTime;
        }

        public boolean isIsRemark() {
            return isRemark;
        }

        public void setIsRemark(boolean isRemark) {
            this.isRemark = isRemark;
        }
    }

    public static class ReverbList {
        private String data;
        private boolean echo;
        private boolean equalizer;
        private int index;
        private String name;
        private boolean reverb;
        private int type;
        private String icon;
        private int id;
        private String icon1;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public boolean isEcho() {
            return echo;
        }

        public void setEcho(boolean echo) {
            this.echo = echo;
        }

        public boolean isEqualizer() {
            return equalizer;
        }

        public void setEqualizer(boolean equalizer) {
            this.equalizer = equalizer;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isReverb() {
            return reverb;
        }

        public void setReverb(boolean reverb) {
            this.reverb = reverb;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getIcon1() {
            return icon1;
        }

        public void setIcon1(String icon1) {
            this.icon1 = icon1;
        }
    }

    public static class EqualizerEffects {
        private String data;
        private boolean echo;
        private boolean equalizer;
        private int index;
        private String name;
        private boolean reverb;
        private int type;
        private String icon;
        private int id;
        private String icon1;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public boolean isEcho() {
            return echo;
        }

        public void setEcho(boolean echo) {
            this.echo = echo;
        }

        public boolean isEqualizer() {
            return equalizer;
        }

        public void setEqualizer(boolean equalizer) {
            this.equalizer = equalizer;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isReverb() {
            return reverb;
        }

        public void setReverb(boolean reverb) {
            this.reverb = reverb;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getIcon1() {
            return icon1;
        }

        public void setIcon1(String icon1) {
            this.icon1 = icon1;
        }
    }

    public static class BoardEffects {
        private String data;
        private boolean echo;
        private boolean equalizer;
        private int index;
        private String name;
        private boolean reverb;
        private int type;
        private String icon;
        private int id;
        private String icon1;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public boolean isEcho() {
            return echo;
        }

        public void setEcho(boolean echo) {
            this.echo = echo;
        }

        public boolean isEqualizer() {
            return equalizer;
        }

        public void setEqualizer(boolean equalizer) {
            this.equalizer = equalizer;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isReverb() {
            return reverb;
        }

        public void setReverb(boolean reverb) {
            this.reverb = reverb;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getIcon1() {
            return icon1;
        }

        public void setIcon1(String icon1) {
            this.icon1 = icon1;
        }
    }

    public static class TempEffects {
        private String data;
        private boolean echo;
        private boolean equalizer;
        private int index;
        private String name;
        private boolean reverb;
        private int type;
        private String icon;
        private int id;
        private String icon1;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public boolean isEcho() {
            return echo;
        }

        public void setEcho(boolean echo) {
            this.echo = echo;
        }

        public boolean isEqualizer() {
            return equalizer;
        }

        public void setEqualizer(boolean equalizer) {
            this.equalizer = equalizer;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isReverb() {
            return reverb;
        }

        public void setReverb(boolean reverb) {
            this.reverb = reverb;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getIcon1() {
            return icon1;
        }

        public void setIcon1(String icon1) {
            this.icon1 = icon1;
        }
    }
}
