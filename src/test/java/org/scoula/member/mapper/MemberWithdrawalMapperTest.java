package org.scoula.member.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberWithdrawalMapperTest {

    @Test
    void detachesOnlyOtherUsersRepliesFromWithdrawingUsersParents() throws Exception {
        Configuration configuration = parseMapper();

        String sql = sql(configuration, "detachOtherUsersReplies");

        assertTrue(sql.contains("parent.id = child.parent_comment_id"));
        assertTrue(sql.contains("parent.user_id = ?"));
        assertTrue(sql.contains("child.user_id != ?"));
        assertTrue(sql.contains("SET child.parent_comment_id = NULL"));
    }

    @Test
    void deletesInquiriesExplicitlyInsteadOfRelyingOnSetNullFk() throws Exception {
        Configuration configuration = parseMapper();

        String sql = sql(configuration, "deleteInquiriesByUserId");

        assertTrue(sql.contains("DELETE FROM inquiries"));
        assertTrue(sql.contains("WHERE user_id = ?"));
        assertFalse(sql.contains("UPDATE inquiries"));
    }

    @Test
    void doesNotDefinePlaceImageDeletionForWithdrawal() throws Exception {
        Configuration configuration = parseMapper();

        assertFalse(configuration.hasStatement(
                MemberWithdrawalMapper.class.getName() + ".deletePlaceImagesByUserId"));
    }

    private Configuration parseMapper() throws Exception {
        Configuration configuration = new Configuration();
        String path = "org/scoula/member/mapper/MemberWithdrawalMapper.xml";
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            new XMLMapperBuilder(input, configuration, path,
                    configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private String sql(Configuration configuration, String id) {
        return configuration.getMappedStatement(MemberWithdrawalMapper.class.getName() + "." + id)
                .getBoundSql(Map.of("userId", 7L))
                .getSql().replaceAll("\\s+", " ");
    }
}
