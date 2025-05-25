import * as shared from "shared-lib";

export const TokenInfo = shared.com.ilogos.shared.model.TokenInfo
type Companion = typeof shared.com.ilogos.shared.model.TokenInfo.Companion;
export const TokenInfoCompanion = (shared.com.ilogos.shared.model.AbstractTokenInfo as unknown as Companion);

export const getTokenInfo = shared.com.ilogos.shared.utils.TokenInfoUtils.createInfo