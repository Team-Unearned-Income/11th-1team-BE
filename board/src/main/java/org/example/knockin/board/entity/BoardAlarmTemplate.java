package org.example.knockin.board.entity;

import org.example.knockin.global.entity.AlarmMessageTemplate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BoardAlarmTemplate implements AlarmMessageTemplate {
    DELETE_BOARD("게시글 비공개 안내", "게시글이 비공개 처리되었습니다. 사유: %s", "knockinrn://explore"),
    RECOVER_BOARD("게시글이 다시 노출됩니다", "비공개 처리된 게시글이 다시 노출됩니다.", "knockinrn://room/%s"),
    ;

    private final String title;
    private final String contents;
    private final String deepLink;

    @Override
    public String formatTitle(Object... args) {
        return String.format(this.title, args);
    }

    @Override
    public String formatContents(Object... args) {
        return String.format(this.contents, args);
    }

    @Override
    public String formatDeepLink(Object... args) {
        return String.format(this.deepLink, args);
    }
}
