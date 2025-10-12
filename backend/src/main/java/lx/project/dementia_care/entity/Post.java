// // src/main/java/lx/project/dementia_care/entity/Post.java

// package lx.project.dementia_care.entity;

// import jakarta.persistence.*;
// import java.time.OffsetDateTime;
// import lombok.Getter;
// import lombok.NoArgsConstructor;
// import lombok.Setter;

// @Getter
// @Setter
// @NoArgsConstructor // JPA가 객체를 생성할 때 필요한 기본 생성자를 만듭니다.
// @Entity
// @Table(name = "post")
// public class Post {

//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Integer postId;

//     @Column(nullable = false, length = 255)
//     private String title;

//     private Integer userId;

//     @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE", insertable = false, updatable = false)
//     private OffsetDateTime createdAt;

//     @Column(columnDefinition = "TEXT")
//     private String content;

//     @Column(name = "view_count", columnDefinition = "INT DEFAULT 0")
//     private Integer views;

// //    @Column(columnDefinition = "INT DEFAULT 0")
// //    private Integer likes;

//     @Column(name = "image_path", length = 255)
//     private String image;
    
// }