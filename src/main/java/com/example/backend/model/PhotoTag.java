package com.example.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "photo_tag")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoTag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "photo_id", nullable = false)
    private Long photoId;
    
    @Column(name = "tag_id", nullable = false)
    private Long tagId;
} 