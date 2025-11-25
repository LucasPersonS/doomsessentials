import os
import re
import sys
import json
import argparse
import datetime
import shutil

def strip_json_comments(text):
    text = re.sub(r'(?m)//.*$', '', text)
    text = re.sub(r'(?s)/\*.*?\*/', '', text)
    text = re.sub(r',\s*([}\]])', r'\1', text)
    return text

def load_json_file(path):
    try:
        with open(path, 'r', encoding='utf-8') as f:
            raw = f.read()
        clean = strip_json_comments(raw)
        return json.loads(clean), raw
    except Exception as e:
        return None, str(e)

def write_json_file(path, obj):
    d = os.path.dirname(path)
    if not os.path.exists(d):
        os.makedirs(d, exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(json.dumps(obj, ensure_ascii=False, indent=2))

def list_pack_dirs(root):
    tacz_root = os.path.join(root, 'bin', 'main', 'data', 'packs', 'tacz')
    if not os.path.isdir(tacz_root):
        return []
    return [os.path.join(tacz_root, name) for name in os.listdir(tacz_root) if os.path.isdir(os.path.join(tacz_root, name))]

def read_namespace(pack_dir):
    meta_path = os.path.join(pack_dir, 'gunpack.meta.json')
    j, _ = load_json_file(meta_path)
    if not j:
        return None
    ns = j.get('namespace')
    return ns

def index_ids(base_dir):
    ids = set()
    if not os.path.isdir(base_dir):
        return ids
    for name in os.listdir(base_dir):
        if name.endswith('.json'):
            ids.add(os.path.splitext(name)[0])
    return ids

def build_global_indexes(pack_infos):
    ammo_by_ns = {}
    attach_by_ns = {}
    for info in pack_infos:
        ns = info['ns']
        data_ns = os.path.join(info['dir'], 'data', ns)
        ammo_idx = index_ids(os.path.join(data_ns, 'index', 'ammo'))
        att_idx = index_ids(os.path.join(data_ns, 'index', 'attachments'))
        ammo_by_ns[ns] = ammo_idx
        attach_by_ns[ns] = att_idx
    ammo_global = {}
    for ns, ids in ammo_by_ns.items():
        for i in ids:
            ammo_global.setdefault(i, set()).add(ns)
    attach_global = {}
    for ns, ids in attach_by_ns.items():
        for i in ids:
            attach_global.setdefault(i, set()).add(ns)
    return ammo_by_ns, attach_by_ns, ammo_global, attach_global

def rewrite_multi_value(v, src_ns_list, target_ns):
    if isinstance(v, str):
        v2 = v
        for src_ns in src_ns_list:
            v2 = re.sub(r"\b" + re.escape(src_ns) + r":", target_ns + ":", v2)
            v2 = re.sub(r"\b" + re.escape(src_ns) + r"\.", target_ns + ".", v2)
        return v2
    if isinstance(v, list):
        return [rewrite_multi_value(x, src_ns_list, target_ns) for x in v]
    if isinstance(v, dict):
        return {k: rewrite_multi_value(vv, src_ns_list, target_ns) for k, vv in v.items()}
    return v

def rewrite_json_obj(obj, src_ns_list, target_ns):
    if isinstance(obj, dict):
        return {k: rewrite_multi_value(v, src_ns_list, target_ns) for k, v in obj.items()}
    if isinstance(obj, list):
        return [rewrite_multi_value(v, src_ns_list, target_ns) for v in obj]
    return obj

def ensure_dir(path):
    d = os.path.dirname(path)
    os.makedirs(d, exist_ok=True)

def copy_json_with_rewrite(src, dst, src_ns_list, target_ns, dry_run):
    obj, raw_or_err = load_json_file(src)
    if obj is None:
        return {'path': src, 'status': 'error', 'error': raw_or_err}
    new_obj = rewrite_json_obj(obj, src_ns_list, target_ns)
    if not dry_run:
        ensure_dir(dst)
        write_json_file(dst, new_obj)
    return {'path': src, 'target': dst, 'status': 'ok', 'changes': ['rewrite_ns']}

def copy_file(src, dst, dry_run):
    if not dry_run:
        ensure_dir(dst)
        shutil.copy2(src, dst)
    return {'path': src, 'target': dst, 'status': 'ok', 'changes': ['copy']}

def standardize_ammo(value, pack_ns, ammo_global, prefer_ns_order):
    if not isinstance(value, str):
        return value, ['ammo_type_invalid']
    v = value.strip()
    m = re.match(r'^(endless[_ ]?ammo)\s*:?\s*(.*)$', v, flags=re.IGNORECASE)
    if m:
        rest = m.group(2).strip()
        if rest:
            return f"ea:{rest}", [f"ammo_ns_rewritten:{value}->ea:{rest}"]
        else:
            return v, ["ammo_missing_id_after_endless"]
    if ':' in v:
        parts = v.split(':', 1)
        ns, aid = parts[0], parts[1]
        if aid in ammo_global and ns not in ammo_global[aid]:
            return v, [f"ammo_ns_mismatch:{ns}:{aid}"]
        return v, []
    aid = v
    if aid in ammo_global:
        for ns in prefer_ns_order:
            if ns in ammo_global[aid]:
                return f"{ns}:{aid}", [f"ammo_ns_added:{ns}:{aid}"]
    return v, ["ammo_no_namespace"]

def fix_attachments(data_obj, attach_global, prefer_ns_order):
    changes = []
    allowed = {"scope","stock","muzzle","grip","laser","extended_mag"}
    ats = data_obj.get('allow_attachment_types')
    if ats is None:
        data_obj['allow_attachment_types'] = []
        changes.append('add_allow_attachment_types')
    elif not isinstance(ats, list):
        data_obj['allow_attachment_types'] = []
        changes.append('allow_attachment_types_to_list')
    else:
        norm = []
        seen = set()
        for a in ats:
            if isinstance(a, str):
                a2 = a.strip()
                if a2 in allowed and a2 not in seen:
                    norm.append(a2)
                    seen.add(a2)
        if norm != ats:
            data_obj['allow_attachment_types'] = norm
            changes.append('allow_attachment_types_normalized')
    ex = data_obj.get('exclusive_attachments')
    if ex is None:
        data_obj['exclusive_attachments'] = {}
        changes.append('add_exclusive_attachments')
        ex = data_obj['exclusive_attachments']
    if not isinstance(ex, dict):
        data_obj['exclusive_attachments'] = {}
        changes.append('exclusive_attachments_to_object')
        ex = data_obj['exclusive_attachments']
    new_ex = {}
    for k, v in list(ex.items()):
        kid = str(k).strip()
        if ':' not in kid:
            base = kid
            ns_sel = None
            if base in attach_global:
                for ns in prefer_ns_order:
                    if ns in attach_global[base]:
                        ns_sel = ns
                        break
            if ns_sel:
                kid = f"{ns_sel}:{base}"
                changes.append(f'exclusive_attachment_id_ns_added:{k}->{kid}')
        if isinstance(v, dict):
            new_ex[kid] = v
        else:
            new_ex[kid] = {}
            changes.append(f'exclusive_attachment_value_object_fix:{kid}')
    if new_ex != ex:
        data_obj['exclusive_attachments'] = new_ex
    return changes

def process_gun_data(path, pack_ns, ammo_global, attach_global, prefer_ns_order, dry_run):
    obj, raw_or_err = load_json_file(path)
    if obj is None:
        return {'path': path, 'status': 'error', 'error': raw_or_err}
    changes = []
    if 'ammo' in obj:
        new_ammo, ammo_changes = standardize_ammo(obj['ammo'], pack_ns, ammo_global, prefer_ns_order)
        if ammo_changes:
            changes.extend(ammo_changes)
        if new_ammo != obj['ammo']:
            obj['ammo'] = new_ammo
            changes.append('ammo_updated')
    changes.extend(fix_attachments(obj, attach_global, prefer_ns_order))
    if changes and not dry_run:
        write_json_file(path, obj)
    return {'path': path, 'status': 'ok', 'changes': changes}

def process_display_json(path, dry_run):
    obj, raw_or_err = load_json_file(path)
    if obj is None:
        return {'path': path, 'status': 'error', 'error': raw_or_err}
    return {'path': path, 'status': 'ok', 'changes': []}

def migrate_pack(info, target_pack_dir, target_ns, prefer_ns_order, dry_run, conflict, rewrite_ns_list):
    src_ns = info['ns']
    pack_dir = info['dir']
    d_ns = os.path.join(pack_dir, 'data', src_ns)
    a_ns = os.path.join(pack_dir, 'assets', src_ns)
    res = {'pack': os.path.basename(pack_dir), 'namespace': src_ns, 'copied': [], 'errors': []}
    guns_idx_dir = os.path.join(d_ns, 'index', 'guns')
    guns_data_dir = os.path.join(d_ns, 'data', 'guns')
    guns_disp_dir = os.path.join(a_ns, 'display', 'guns')
    ammo_idx_dir = os.path.join(d_ns, 'index', 'ammo')
    ammo_disp_dir = os.path.join(a_ns, 'display', 'ammo')
    recipes_gun_dir = os.path.join(d_ns, 'recipes', 'gun')
    tgt_data_ns = os.path.join(target_pack_dir, 'data', target_ns)
    tgt_assets_ns = os.path.join(target_pack_dir, 'assets', target_ns)
    if os.path.isdir(guns_idx_dir):
        for name in os.listdir(guns_idx_dir):
            if not name.endswith('.json'):
                continue
            gun_id = os.path.splitext(name)[0]
            src_idx = os.path.join(guns_idx_dir, name)
            dst_idx = os.path.join(tgt_data_ns, 'index', 'guns', name)
            if os.path.exists(dst_idx) and conflict == 'skip':
                continue
            if os.path.exists(dst_idx) and conflict == 'dup':
                n = 1
                base = gun_id
                while True:
                    dup_name = f"{base}_dup{n}.json"
                    dst_try = os.path.join(tgt_data_ns, 'index', 'guns', dup_name)
                    if not os.path.exists(dst_try):
                        dst_idx = dst_try
                        break
                    n += 1
            r = copy_json_with_rewrite(src_idx, dst_idx, rewrite_ns_list, target_ns, dry_run)
            res['copied'].append(r)
            src_data = os.path.join(guns_data_dir, gun_id + '_data.json')
            dst_data = os.path.join(tgt_data_ns, 'data', 'guns', os.path.basename(dst_idx).replace('.json', '_data.json') if '_dup' in os.path.basename(dst_idx) else gun_id + '_data.json')
            if os.path.exists(src_data):
                r2 = copy_json_with_rewrite(src_data, dst_data, rewrite_ns_list, target_ns, dry_run)
                res['copied'].append(r2)
            src_disp = os.path.join(guns_disp_dir, gun_id + '_display.json')
            dst_disp = os.path.join(tgt_assets_ns, 'display', 'guns', os.path.basename(dst_idx).replace('.json', '_display.json') if '_dup' in os.path.basename(dst_idx) else gun_id + '_display.json')
            if os.path.exists(src_disp):
                r3 = copy_json_with_rewrite(src_disp, dst_disp, rewrite_ns_list, target_ns, dry_run)
                res['copied'].append(r3)
    if os.path.isdir(ammo_idx_dir):
        for name in os.listdir(ammo_idx_dir):
            if not name.endswith('.json'):
                continue
            src_ai = os.path.join(ammo_idx_dir, name)
            dst_ai = os.path.join(tgt_data_ns, 'index', 'ammo', name)
            if os.path.exists(dst_ai) and conflict == 'skip':
                pass
            else:
                if os.path.exists(dst_ai) and conflict == 'dup':
                    n = 1
                    base = os.path.splitext(name)[0]
                    while True:
                        dup_name = f"{base}_dup{n}.json"
                        dst_try = os.path.join(tgt_data_ns, 'index', 'ammo', dup_name)
                        if not os.path.exists(dst_try):
                            dst_ai = dst_try
                            break
                        n += 1
                r = copy_json_with_rewrite(src_ai, dst_ai, rewrite_ns_list, target_ns, dry_run)
                res['copied'].append(r)
    if os.path.isdir(ammo_disp_dir):
        for name in os.listdir(ammo_disp_dir):
            if not name.endswith('.json'):
                continue
            src_ad = os.path.join(ammo_disp_dir, name)
            dst_ad = os.path.join(tgt_assets_ns, 'display', 'ammo', name)
            if os.path.exists(dst_ad) and conflict == 'skip':
                pass
            else:
                if os.path.exists(dst_ad) and conflict == 'dup':
                    n = 1
                    base = os.path.splitext(name)[0]
                    while True:
                        dup_name = f"{base}_dup{n}.json"
                        dst_try = os.path.join(tgt_assets_ns, 'display', 'ammo', dup_name)
                        if not os.path.exists(dst_try):
                            dst_ad = dst_try
                            break
                        n += 1
                r = copy_json_with_rewrite(src_ad, dst_ad, rewrite_ns_list, target_ns, dry_run)
                res['copied'].append(r)
    if os.path.isdir(recipes_gun_dir):
        for name in os.listdir(recipes_gun_dir):
            if not name.endswith('.json'):
                continue
            src_rg = os.path.join(recipes_gun_dir, name)
            dst_rg = os.path.join(tgt_data_ns, 'recipes', 'gun', name)
            r = copy_json_with_rewrite(src_rg, dst_rg, rewrite_ns_list, target_ns, dry_run)
            res['copied'].append(r)
    return res

def copy_tree_with_rewrite_json(src_root, dst_root, rewrite_ns_list, target_ns, dry_run, conflict):
    results = []
    for dirpath, dirnames, filenames in os.walk(src_root):
        rel = os.path.relpath(dirpath, src_root)
        dst_dir = os.path.join(dst_root, rel) if rel != '.' else dst_root
        for fname in filenames:
            src_f = os.path.join(dirpath, fname)
            dst_f = os.path.join(dst_dir, fname)
            if os.path.exists(dst_f) and conflict == 'skip':
                continue
            if fname.lower().endswith('.json'):
                r = copy_json_with_rewrite(src_f, dst_f, rewrite_ns_list, target_ns, dry_run)
                results.append(r)
            else:
                r = copy_file(src_f, dst_f, dry_run)
                results.append(r)
    return results

def migrate_namespace_full(info, target_pack_dir, target_ns, rewrite_ns_list, dry_run, conflict):
    src_ns = info['ns']
    pack_dir = info['dir']
    d_ns = os.path.join(pack_dir, 'data', src_ns)
    a_ns = os.path.join(pack_dir, 'assets', src_ns)
    tgt_data_ns = os.path.join(target_pack_dir, 'data', target_ns)
    tgt_assets_ns = os.path.join(target_pack_dir, 'assets', target_ns)
    res = {'pack': os.path.basename(pack_dir), 'namespace': src_ns, 'copied': [], 'errors': []}
    if os.path.isdir(d_ns):
        res['copied'].extend(copy_tree_with_rewrite_json(d_ns, tgt_data_ns, rewrite_ns_list, target_ns, dry_run, conflict))
    if os.path.isdir(a_ns):
        res['copied'].extend(copy_tree_with_rewrite_json(a_ns, tgt_assets_ns, rewrite_ns_list, target_ns, dry_run, conflict))
    return res

def rewrite_in_place_dir(base_dir, rewrite_ns_list, target_ns, dry_run):
    changes = []
    for dirpath, dirnames, filenames in os.walk(base_dir):
        for fname in filenames:
            if not fname.lower().endswith('.json'):
                continue
            p = os.path.join(dirpath, fname)
            obj, raw_or_err = load_json_file(p)
            if obj is None:
                continue
            new_obj = rewrite_json_obj(obj, rewrite_ns_list, target_ns)
            if new_obj != obj and not dry_run:
                write_json_file(p, new_obj)
                changes.append({'path': p, 'changes': ['in_place_rewrite_ns']})
    return changes

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', default=os.getcwd())
    ap.add_argument('--dry-run', action='store_true')
    ap.add_argument('--report', default=None)
    ap.add_argument('--migrate', action='store_true')
    ap.add_argument('--target-pack', default='Doomsday')
    ap.add_argument('--conflict', choices=['skip','dup'], default='skip')
    args = ap.parse_args()
    pack_dirs = list_pack_dirs(args.root)
    pack_infos = []
    for pd in pack_dirs:
        ns = read_namespace(pd)
        if not ns:
            continue
        if os.path.basename(pd) == 'tacz_default_gun':
            continue
        pack_infos.append({'dir': pd, 'ns': ns})
    ammo_by_ns, attach_by_ns, ammo_global, attach_global = build_global_indexes(pack_infos)
    prefer_ns_order = ['doomsday', 'tacz', 'ea']
    results = {'started_at': datetime.datetime.now().isoformat(), 'dry_run': args.dry_run, 'packs': [], 'errors': [], 'changes': 0, 'migrations': []}
    if args.migrate:
        target_pack_dir = os.path.join(args.root, 'bin', 'main', 'data', 'packs', 'tacz', args.target_pack)
        rewrite_ns_list = set([info['ns'] for info in pack_infos]) | set(['ea'])
        for info in pack_infos:
            if os.path.basename(info['dir']).lower() == args.target_pack.lower():
                continue
            if info['ns'].lower() == 'ea':
                mig = migrate_namespace_full(info, target_pack_dir, 'doomsday', rewrite_ns_list, args.dry_run, args.conflict)
            else:
                mig = migrate_pack(info, target_pack_dir, 'doomsday', prefer_ns_order, args.dry_run, args.conflict, rewrite_ns_list)
            results['migrations'].append(mig)
        in_place_changes = rewrite_in_place_dir(os.path.join(target_pack_dir), rewrite_ns_list, 'doomsday', args.dry_run)
        if in_place_changes:
            results['migrations'].append({'pack': args.target_pack, 'namespace': 'doomsday', 'copied': in_place_changes, 'errors': []})
    else:
        for info in pack_infos:
            ns = info['ns']
            d_ns = os.path.join(info['dir'], 'data', ns)
            a_ns = os.path.join(info['dir'], 'assets', ns)
            guns_dir = os.path.join(d_ns, 'data', 'guns')
            disp_guns_dir = os.path.join(a_ns, 'display', 'guns')
            pack_res = {'pack': os.path.basename(info['dir']), 'namespace': ns, 'files': []}
            if os.path.isdir(guns_dir):
                for name in os.listdir(guns_dir):
                    if name.endswith('_data.json'):
                        p = os.path.join(guns_dir, name)
                        r = process_gun_data(p, ns, ammo_global, attach_global, prefer_ns_order, args.dry_run)
                        pack_res['files'].append(r)
                        if r.get('changes'):
                            results['changes'] += len([c for c in r['changes'] if not c.startswith('ammo_ns_mismatch')])
                        if r.get('status') == 'error':
                            results['errors'].append(r)
            if os.path.isdir(disp_guns_dir):
                for name in os.listdir(disp_guns_dir):
                    if name.endswith('_display.json'):
                        p = os.path.join(disp_guns_dir, name)
                        r = process_display_json(p, args.dry_run)
                        pack_res['files'].append(r)
                        if r.get('status') == 'error':
                            results['errors'].append(r)
            results['packs'].append(pack_res)
    results['finished_at'] = datetime.datetime.now().isoformat()
    report_path = args.report
    if not report_path:
        ts = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
        report_dir = os.path.join(args.root, 'scripts', 'reports')
        os.makedirs(report_dir, exist_ok=True)
        report_path = os.path.join(report_dir, f'report_json_fix_{ts}.json')
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(json.dumps(results, ensure_ascii=False, indent=2))
    print(report_path)

if __name__ == '__main__':
    main()

