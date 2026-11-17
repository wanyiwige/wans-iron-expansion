package cn.wanyiwige.astralcompat.client.item;

import cn.wanyiwige.astralcompat.item.AstralLanceItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class AstralLanceRenderer extends GeoItemRenderer<AstralLanceItem> {
    public AstralLanceRenderer() {
        super(new AstralLanceModel());
    }
}
