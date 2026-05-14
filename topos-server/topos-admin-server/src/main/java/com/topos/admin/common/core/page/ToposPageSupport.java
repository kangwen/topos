package com.topos.admin.common.core.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.topos.admin.common.constant.HttpStatus;

import java.util.List;

/**
 * 管理端列表分页与 {@link TableDataInfo} 组装。
 */
public final class ToposPageSupport {

	private ToposPageSupport() {
	}

	public static TableDataInfo of(Page<?> page) {
		TableDataInfo t = new TableDataInfo();
		t.setCode(HttpStatus.SUCCESS);
		t.setMsg("查询成功");
		t.setRows(page.getRecords());
		t.setTotal(page.getTotal());
		return t;
	}

	public static TableDataInfo ofAll(List<?> rows) {
		TableDataInfo t = new TableDataInfo();
		t.setCode(HttpStatus.SUCCESS);
		t.setMsg("查询成功");
		t.setRows(rows);
		t.setTotal(rows == null ? 0 : rows.size());
		return t;
	}
}
