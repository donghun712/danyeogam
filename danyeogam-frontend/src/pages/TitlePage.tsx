import { TitleSection } from "@/components/title/TitleSection";
import styles from "./TitlePage.module.css";

/**
 * 화면시안 "07 칭호" — 원래 도감 화면 맨 아래에 붙어 있었는데, 실제 테스트에서 도감
 * 카드 전체를 스크롤해서 지나야 보인다는 피드백을 받아 별도 탭으로 분리했다.
 * TitleSection 자체가 헤더(아이콘 + "칭호" + 획득 개수)를 갖고 있어서 페이지에서
 * 중복으로 만들지 않는다.
 */
export function TitlePage() {
  return (
    <div className={styles.container}>
      <TitleSection />
    </div>
  );
}
