package com.topos.admin.topos.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端保存家长商城商品：图片以数组提交，落库为 JSON。
 */
public class ParentShopProductBody {

    private Long id;
    private String name;
    /** 店铺名称，可选，最多 20 字 */
    private String shopName;
    private String thirdParty;
    private String thirdLink;
    private BigDecimal price;
    private List<String> images;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getThirdParty() {
        return thirdParty;
    }

    public void setThirdParty(String thirdParty) {
        this.thirdParty = thirdParty;
    }

    public String getThirdLink() {
        return thirdLink;
    }

    public void setThirdLink(String thirdLink) {
        this.thirdLink = thirdLink;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}
