package com.cutsame.ui.utils;

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
                mediaItem.volume
        );
    }

    public static ArrayList<MediaItem> cutSameToOlaMediaItemList(ArrayList<com.ss.android.ugc.cut_ui.MediaItem> mediaItemList) {

        ArrayList<MediaItem> olaMediaItemList = new ArrayList<>();
        for (com.ss.android.ugc.cut_ui.MediaItem mediaItem : mediaItemList) {
            olaMediaItemList.add(cutSameToOlaMediaItem(mediaItem));
        }
        return olaMediaItemList;
    }
}
