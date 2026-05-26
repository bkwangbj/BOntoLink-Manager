package com.beiktech.bontolink.mapper;

import com.beiktech.bontolink.entity.IconLibGroup;
import com.beiktech.bontolink.entity.IconLibIcon;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface IconLibMapper {

    /* ===== Group ===== */

    @Select("SELECT id, parent_id AS parentId, name, sort, source, create_time AS createTime, update_time AS updateTime " +
            " FROM icon_lib_group ORDER BY COALESCE(parent_id,''), sort, create_time")
    List<IconLibGroup> listGroups();

    @Select("SELECT id, parent_id AS parentId, name, sort, source, create_time AS createTime, update_time AS updateTime " +
            " FROM icon_lib_group WHERE id = #{id}")
    IconLibGroup findGroup(@Param("id") String id);

    @Select("SELECT COUNT(*) FROM icon_lib_group")
    int countGroups();

    @Select("SELECT COUNT(*) FROM icon_lib_icon")
    int countIcons();

    @Select("SELECT COUNT(*) FROM icon_lib_icon WHERE group_id = #{groupId}")
    int countIconsByGroup(@Param("groupId") String groupId);

    @Insert("INSERT INTO icon_lib_group(id, parent_id, name, sort, source) VALUES(#{id},#{parentId},#{name},#{sort},#{source})")
    void insertGroup(IconLibGroup g);

    @Update("UPDATE icon_lib_group SET name=#{name}, sort=#{sort}, update_time=datetime('now','localtime') WHERE id=#{id}")
    void updateGroup(IconLibGroup g);

    @Delete("DELETE FROM icon_lib_group WHERE id = #{id}")
    void deleteGroup(@Param("id") String id);

    /** 删除一组节点(用于级联删除子组) */
    @Delete("<script>DELETE FROM icon_lib_group WHERE id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach></script>")
    void deleteGroupBatch(@Param("ids") java.util.Collection<String> ids);

    /* ===== Icon ===== */

    @Select("SELECT id, group_id AS groupId, name, view_box AS viewBox, content, sort, " +
            " create_time AS createTime, update_time AS updateTime " +
            " FROM icon_lib_icon ORDER BY group_id, sort, create_time")
    List<IconLibIcon> listIcons();

    @Select("SELECT id, group_id AS groupId, name, view_box AS viewBox, content, sort " +
            " FROM icon_lib_icon WHERE group_id = #{groupId} ORDER BY sort, create_time")
    List<IconLibIcon> listIconsByGroup(@Param("groupId") String groupId);

    @Insert("INSERT INTO icon_lib_icon(id, group_id, name, view_box, content, sort) " +
            " VALUES(#{id},#{groupId},#{name},#{viewBox},#{content},#{sort})")
    void insertIcon(IconLibIcon ic);

    @Delete("DELETE FROM icon_lib_icon WHERE id = #{id}")
    void deleteIcon(@Param("id") String id);

    @Delete("<script>DELETE FROM icon_lib_icon WHERE id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach></script>")
    void deleteIconBatch(@Param("ids") java.util.Collection<String> ids);

    @Delete("<script>DELETE FROM icon_lib_icon WHERE group_id IN " +
            "<foreach collection='groupIds' item='i' open='(' separator=',' close=')'>#{i}</foreach></script>")
    void deleteIconsByGroups(@Param("groupIds") java.util.Collection<String> groupIds);
}
