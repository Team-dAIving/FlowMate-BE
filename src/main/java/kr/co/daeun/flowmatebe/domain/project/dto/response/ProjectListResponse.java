package kr.co.daeun.flowmatebe.domain.project.dto.response;

import java.util.List;

public record ProjectListResponse(
        List<ProjectListItemResponse> projects
) {
}
