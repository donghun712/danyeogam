import { apiRequest } from "@/api/client";
import type { TitleList } from "@/types/api";

/**
 * 내 칭호 목록. 활성 칭호 정의 전체(획득 여부 무관)를 표시 순서대로 반환하므로
 * 프론트는 별도의 정적 칭호 목록을 갖지 않는다. 스탬프 인증 응답의 newTitleIds가
 * 비어 있지 않으면 다시 호출해서 새로 획득한 칭호의 이름/설명을 매칭한다.
 */
export function fetchTitles(): Promise<TitleList> {
  return apiRequest<TitleList>("/me/titles");
}
