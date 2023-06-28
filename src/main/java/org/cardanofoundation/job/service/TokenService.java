package org.cardanofoundation.job.service;

import org.springframework.data.domain.Pageable;

import org.cardanofoundation.job.dto.BaseFilterDto;
import org.cardanofoundation.job.dto.token.TokenFilterDto;

public interface TokenService {

  BaseFilterDto<TokenFilterDto> filterToken(Pageable pageable);
}
