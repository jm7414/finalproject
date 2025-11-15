package lx.project.dementia_care.dto;

import java.sql.Timestamp;

public class PlazaNoticeDTO {
    private Integer noticeNo;   // 기존 postId -> noticeNo로 변경
    private String title;
    private String content;
    private Integer authorUserNo;
    private String authorName;
    private Timestamp createdAt;
    private Integer viewCount;

    public Integer getNoticeNo() {
        return noticeNo;
    }

    public void setNoticeNo(Integer noticeNo) {
        this.noticeNo = noticeNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getAuthorUserNo() {
        return authorUserNo;
    }

    public void setAuthorUserNo(Integer authorUserNo) {
        this.authorUserNo = authorUserNo;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }
}
