package com.topos.admin.web.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 前端路由 meta
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaVo {

	private String title;
	private String icon;
	private boolean noCache;
	private String link;

	public MetaVo(String title, String icon) {
		this(title, icon, false, null);
	}
}
