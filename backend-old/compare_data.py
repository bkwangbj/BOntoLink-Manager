"""Compare SQLite vs PG row counts per table"""
import sqlite3, psycopg2

sqlite = sqlite3.connect('bontolink.db')
pg = psycopg2.connect(host='dev.beiktech.com', port=9523, dbname='swc_wx_dev', user='postgres', password='postgres', options='-c search_path=bonto_link_manager')

TABLES = [
    'ont_biz_category', 'ont_biz_namespace', 'ont_biz_namespace_version',
    'ont_biz_group', 'ont_class', 'ont_interface', 'sys_data_source',
    'ont_dict_def', 'ont_value_types', 'ont_valuetypes_usage_config',
    'ont_enum_types', 'ont_dic_type_class', 'ont_type_class_category_dict',
    'ont_struct_types', 'ont_shared_properties', 'ont_property_format',
    'ont_enum_level_code_rule', 'ont_enum_sync_config', 'ont_enum_sync_log',
    'ont_enum_items', 'ont_biz_group_class', 'ont_class_property',
    'ont_class_link', 'ont_class_action', 'ont_interface_property',
    'ont_interface_class', 'ont_class_group', 'ont_class_disjoint_union',
    'ont_property_equivalent', 'ont_property_disjoint', 'ont_class_ds',
    'ont_struct_items', 'ont_link_types', 'ont_link_mappings',
    'ont_type_class', 'ont_type_class_bind', 'ont_dict_item',
    'ont_physical_table',
]

print(f"{'Table':<35} {'SQLite':>8} {'PG':>8} {'Diff':>8}")
print("-"*60)
total_sqlite = 0
total_pg = 0
for t in TABLES:
    try:
        cur = sqlite.execute(f'SELECT COUNT(*) FROM "{t}"')
        sc = cur.fetchone()[0]
    except:
        sc = -1

    try:
        cur = pg.cursor()
        cur.execute(f'SELECT COUNT(*) FROM "{t}"')
        pc = cur.fetchone()[0]
    except:
        pc = -1

    diff = sc - pc
    if diff != 0:
        print(f"{t:<35} {sc:>8} {pc:>8} {'*** ' + str(diff):>8}")
    else:
        print(f"{t:<35} {sc:>8} {pc:>8} {diff:>8}")
    total_sqlite += max(sc, 0)
    total_pg += max(pc, 0)

print("-"*60)
print(f"{'TOTAL':<35} {total_sqlite:>8} {total_pg:>8} {total_sqlite-total_pg:>8}")
sqlite.close()
pg.close()
