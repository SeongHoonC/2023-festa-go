package com.festago.student.domain;

import com.festago.common.domain.BaseTimeEntity;
import com.festago.common.exception.ErrorCode;
import com.festago.common.exception.InternalServerException;
import com.festago.member.domain.Member;
import com.festago.school.domain.School;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Student extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    private Member member;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private School school;

    @Size(max = 255)
    @NotNull
    private String username;

    public Student(Member member, School school, String username) {
        this(null, member, school, username);
    }

    public Student(Long id, Member member, School school, String username) {
        validate(member, school, username);
        this.id = id;
        this.member = member;
        this.school = school;
        this.username = username;
    }

    private void validate(Member member, School school, String username) {
        checkNotNull(member, school, username);
        checkLength(username);
    }

    private void checkNotNull(Member member, School school, String username) {
        if (member == null ||
            school == null ||
            username == null) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void checkLength(String username) {
        if (overLength(username, 255)) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean overLength(String target, int maxLength) {
        if (target == null) {
            return false;
        }
        return target.length() > maxLength;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public School getSchool() {
        return school;
    }

    public String getUsername() {
        return username;
    }
}
