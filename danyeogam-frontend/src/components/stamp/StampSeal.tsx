import { splitNameForSeal, sealFontSize } from "./sealText";
import styles from "./StampSeal.module.css";

interface StampSealProps {
  /** 실제 방문 인증된 관광지명(detail.name) — 로마자 표기 등 없는 데이터는 만들지 않는다. */
  name: string;
}

/**
 * 화면시안 "05 스탬프 획득!" — 손그림 질감의 실제 인장 이미지(public/stamp-seal-ring.png,
 * ChatGPT/Codex 제작, 텍스트 없는 빈 링 + 투명 배경) 위에 관광지명을 코드로 얹는다.
 * 이전에는 SVG로 이중 원 + feTurbulence 노이즈를 흉내 냈는데, 실제 에셋으로 교체했다.
 */
export function StampSeal({ name }: StampSealProps) {
  const lines = splitNameForSeal(name);
  const fontSize = sealFontSize(lines);

  return (
    <div className={styles.seal} role="img" aria-label={`${name} 스탬프`}>
      <img src="/stamp-seal-ring.png" alt="" className={styles.ringImage} />
      <div className={styles.textOverlay}>
        <p className={styles.nameText} style={{ fontSize }}>
          {lines.map((line) => (
            <span key={line} className={styles.nameLine}>
              {line}
            </span>
          ))}
        </p>
        <p className={styles.brandText}>다녀감</p>
      </div>
    </div>
  );
}
