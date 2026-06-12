package com.harun.auth_service.payloads.user.req;

import com.harun.template.BaseSearchPaginateReq;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserSearchRequest extends BaseSearchPaginateReq {
}
