package com.beiktech.bontolink.base.mybatis;

import org.apache.ibatis.reflection.factory.DefaultObjectFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * MyBatis ObjectFactory：强制返回 case-insensitive Map（屏蔽 PG/SQLite 列名大小写差异）
 */
public class CaseInsensitiveMapObjectFactory extends DefaultObjectFactory {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> type) {
        if (Map.class.equals(type)) {
            return (T) new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        }
        return super.create(type);
    }
}
