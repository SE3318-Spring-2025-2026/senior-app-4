package com.spms.backend.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "group_members")
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id;

    @Column(name = "role", length = 50, nullable = false)
    private String role;

    public GroupMember() {}

    public GroupMemberId getId() { return id; }
    public void setId(GroupMemberId id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Embeddable
    public static class GroupMemberId implements Serializable {

        @Column(name = "group_id", nullable = false)
        private Long groupId;

        @Column(name = "user_id", nullable = false)
        private Long userId;

        public GroupMemberId() {}

        public GroupMemberId(Long groupId, Long userId) {
            this.groupId = groupId;
            this.userId = userId;
        }

        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof GroupMemberId)) return false;
            GroupMemberId that = (GroupMemberId) o;
            return Objects.equals(groupId, that.groupId) &&
                   Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, userId);
        }
    }
}
