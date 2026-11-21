package kr.co.daeun.flowmatebe.domain.team.dto.response;

import java.util.List;

public record TeamListResponse(List<TeamResponse> teams) {

    public TeamListResponse {
        teams = List.copyOf(teams);
    }
}
