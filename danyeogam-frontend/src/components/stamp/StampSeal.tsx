import { splitNameForSeal, sealFontSize } from "./sealText";
import styles from "./StampSeal.module.css";

interface StampSealProps {
  /** 실제 방문 인증된 관광지명(detail.name) — 로마자 표기 등 없는 데이터는 만들지 않는다. */
  name: string;
}

/**
 * 화면시안 "05 스탬프 획득!" — 전통 인장을 찍은 듯한 느낌의 성공 그래픽(AI 개발 지침서 6장).
 * 잉크 질감은 SVG feTurbulence로 흉내내고, 살짝 기울여서 손으로 찍은 도장 느낌을 낸다.
 */
export function StampSeal({ name }: StampSealProps) {
  const lines = splitNameForSeal(name);
  const fontSize = sealFontSize(lines);
  const lineHeight = fontSize * 1.15;
  const startY = 100 - ((lines.length - 1) * lineHeight) / 2;

  return (
    <svg
      viewBox="0 0 200 200"
      width="180"
      height="180"
      className={styles.seal}
      role="img"
      aria-label={`${name} 스탬프`}
    >
      <defs>
        <filter id="ink-texture" x="-20%" y="-20%" width="140%" height="140%">
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.9"
            numOctaves="2"
            seed="7"
            result="noise"
          />
          <feDisplacementMap in="SourceGraphic" in2="noise" scale="3.5" />
        </filter>
      </defs>

      <g transform="rotate(-3 100 100)" filter="url(#ink-texture)">
        <circle
          cx="100"
          cy="100"
          r="90"
          fill="none"
          stroke="var(--color-stamp-ink)"
          strokeWidth="3"
        />
        <circle
          cx="100"
          cy="100"
          r="78"
          fill="none"
          stroke="var(--color-stamp-ink)"
          strokeWidth="7"
        />

        <text
          x="100"
          y={startY}
          textAnchor="middle"
          dominantBaseline="middle"
          fill="var(--color-stamp-ink)"
          fontSize={fontSize}
          fontWeight={700}
          className={styles.sealText}
        >
          {lines.map((line, index) => (
            <tspan key={line} x="100" dy={index === 0 ? 0 : lineHeight}>
              {line}
            </tspan>
          ))}
        </text>

        <text
          x="100"
          y="158"
          textAnchor="middle"
          fill="var(--color-stamp-ink)"
          fontSize="11"
          fontWeight={600}
          letterSpacing="2"
        >
          다녀감
        </text>
      </g>
    </svg>
  );
}
