package com.beiktech.bontolink.mapper;

import com.beiktech.bontolink.entity.IconLibGroup;
import com.beiktech.bontolink.entity.IconLibIcon;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 图标库 mapper，覆盖 icon_lib_group（分组）和 icon_lib_icon（图标）两张表。
 */
@Mapper
public interface IconLibMapper {

    /* ===== Group ===== */

    // 查询所有分组，按父节点→排序→创建时间排列（COALESCE 使根节点排在前面）
    @Select("SELECT id, parent_id AS parentId, name, sort, source, create_time AS createTime, update_time AS updateTime " +
            " FROM icon_lib_group ORDER BY COALESCE(parent_id,''), sort, create_time")
    List<IconLibGroup> listGroups();

    // 按 id 查单个分组
    @Select("SELECT id, parent_id AS parentId, name, sort, source, create_time AS createTime, update_time AS updateTime " +
            " FROM icon_lib_group WHERE id = #{id}")
    IconLibGroup findGroup(@Param("id") String id);

    // 统计分组总数
    @Select("SELECT COUNT(*) FROM icon_lib_group")
    int countGroups();

    // 统计图标总数
    @Select("SELECT COUNT(*) FROM icon_lib_icon")
    int countIcons();

    // 统计指定分组下的图标数量
    @Select("SELECT COUNT(*) FROM icon_lib_icon WHERE group_id = #{groupId}")
    int countIconsByGroup(@Param("groupId") String groupId);

    // 新增分组
    @Insert("INSERT INTO icon_lib_group(id, parent_id, name, sort, source) VALUES(#{id},#{parentId},#{name},#{sort},#{source})")
    void insertGroup(IconLibGroup g);

    // 更新分组名称和排序
    @Update("UPDATE icon_lib_group SET name=#{name}, sort=#{sort}, update_time=datetime('now','localtime') WHERE id=#{id}")
    void updateGroup(IconLibGroup g);

    // 删除单个分组
    @Delete("DELETE FROM icon_lib_group WHERE id = #{id}")
    void deleteGroup(@Param("id") String id);

    /** 删除一组节点(用于级联删除子组) */
    @Delete("<script>DELETE FROM icon_lib_group WHERE id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach></script>")
    void deleteGroupBatch(@Param("ids") java.util.Collection<String> ids);

    /* ===== Icon ===== */

    // 查询所有图标，按分组→排序→创建时间排列
    @Select("SELECT id, group_id AS groupId, name, view_box AS viewBox, content, sort, " +
            " create_time AS createTime, update_time AS updateTime " +
            " FROM icon_lib_icon ORDER BY group_id, sort, create_time")
    List<IconLibIcon> listIcons();

    // 查询指定分组下的图标
    @Select("SELECT id, group_id AS groupId, name, view_box AS viewBox, content, sort " +
            " FROM icon_lib_icon WHERE group_id = #{groupId} ORDER BY sort, create_time")
    List<IconLibIcon> listIconsByGroup(@Param("groupId") String groupId);

    // 新增图标
    @Insert("INSERT INTO icon_lib_icon(id, group_id, name, view_box, content, sort) " +
            " VALUES(#{id},#{groupId},#{name},#{viewBox},#{content},#{sort})")
    void insertIcon(IconLibIcon ic);

    // 删除单个图标
    @Delete("DELETE FROM icon_lib_icon WHERE id = #{id}")
    void deleteIcon(@Param("id") String id);

    // 批量删除图标
    @Delete("<script>DELETE FROM icon_lib_icon WHERE id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach></script>")
    void deleteIconBatch(@Param("ids") java.util.Collection<String> ids);

    // 级联删除：删除指定分组列表下的所有图标
    @Delete("<script>DELETE FROM icon_lib_icon WHERE group_id IN " +
            "<foreach collection='groupIds' item='i' open='(' separator=',' close=')'>#{i}</foreach></script>")
    void deleteIconsByGroups(@Param("groupIds") java.util.Collection<String> groupIds);
}
