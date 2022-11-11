package com.ola.chat.picker.utils;

import com.ola.chat.picker.entry.ItemCrop;
import com.ola.chat.picker.entry.MediaItem;

import java.util.ArrayList;

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/17 22:34
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public class CutSameMediaUtils {

    private static MediaItem cutSameToOlaMediaItem(com.ss.android.ugc.cut_ui.MediaItem mediaItem) {

        return new MediaItem(
                mediaItem.materialId,
                mediaItem.targetStartTime,
                mediaItem.isMutable,
                mediaItem.alignMode,
                mediaItem.isSubVideo,
                mediaItem.isReverse,
                mediaItem.cartoonType,
                mediaItem.gamePlayAlgorithm,
                mediaItem.width,
                mediaItem.height,
                mediaItem.duration,
                mediaItem.oriDuration,
                mediaItem.source,
                mediaItem.sourceStartTime,
                mediaItem.cropScale,
                new ItemCrop(mediaItem.crop.lowerRightX, mediaItem.crop.lowerRightY, mediaItem.crop.upperLeftX, mediaItem.crop.upperLeftY),
                mediaItem.type,
                mediaItem.mediaSrcPath,
                mediaItem.targetEndTime,
                mediaItem.volume,
                mediaItem.relation_video_group
        );
    }

    private static com.ss.android.ugc.cut_ui.MediaItem olaMediaItemToCutSame(MediaItem mediaItem) {

        return new com.ss.android.ugc.cut_ui.MediaItem(
                mediaItem.getMaterialId(),
                mediaItem.getTargetStartTime(),
                mediaItem.isMutable(),
                mediaItem.getAlignMode(),
                mediaItem.isSubVideo(),
                mediaItem.isReverse(),
                mediaItem.getCartoonType(),
                mediaItem.getGamePlayAlgorithm(),
                mediaItem.getWidth(),
                mediaItem.getHeight(),
                mediaItem.getDuration(),
                mediaItem.getOriDuration(),
                mediaItem.getSource(),
                mediaItem.getSourceStartTime(),
                mediaItem.getCropScale(),
                new com.ss.android.ugc.cut_ui.ItemCrop(mediaItem.getCrop().getLowerRightX(),
                        mediaItem.getCrop().getLowerRightY(),
                        mediaItem.getCrop().getUpperLeftX(),
                        mediaItem.getCrop().getUpperLeftY()),
                mediaItem.getType(),
                mediaItem.getMediaSrcPath(),
                mediaItem.getTargetEndTime(),
                mediaItem.getVolume(),
                mediaItem.getRelation_video_group()
        );
    }

    public static ArrayList<MediaItem> cutSameToOlaMediaItemList(ArrayList<com.ss.android.ugc.cut_ui.MediaItem> mediaItemList) {

        ArrayList<MediaItem> olaMediaItemList = new ArrayList<>();
        for (com.ss.android.ugc.cut_ui.MediaItem mediaItem : mediaItemList) {
            olaMediaItemList.add(cutSameToOlaMediaItem(mediaItem));
        }
        return olaMediaItemList;
    }

    public static ArrayList<com.ss.android.ugc.cut_ui.MediaItem> olaMediaItemListToCutSame(ArrayList<MediaItem> mediaItemList) {
        ArrayList<com.ss.android.ugc.cut_ui.MediaItem> cutSameMediaItemList = new ArrayList<>();
        for (MediaItem mediaItem : mediaItemList) {
            cutSameMediaItemList.add(olaMediaItemToCutSame(mediaItem));
        }
        return cutSameMediaItemList;
    }
}
