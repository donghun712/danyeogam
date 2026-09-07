/**
 * 인장 안에 들어갈 관광지명을 최대 2줄로 나누고, 길이에 맞는 폰트 크기를 정한다.
 * 백엔드가 로마자 표기(예: "JEONJU")를 내려주지 않으므로, 실제로 있는 한글 이름만 사용한다.
 */
export function splitNameForSeal(name: string): string[] {
  const trimmed = name.trim();
  if (trimmed.length <= 6 || !trimmed.includes(" ")) {
    return [trimmed];
  }

  const mid = trimmed.length / 2;
  let bestIndex = -1;
  let bestDistance = Infinity;
  for (let i = 0; i < trimmed.length; i++) {
    if (trimmed[i] !== " ") continue;
    const distance = Math.abs(i - mid);
    if (distance < bestDistance) {
      bestDistance = distance;
      bestIndex = i;
    }
  }

  if (bestIndex === -1) return [trimmed];
  return [trimmed.slice(0, bestIndex), trimmed.slice(bestIndex + 1)];
}

export function sealFontSize(lines: string[]): number {
  const longest = Math.max(...lines.map((line) => line.length));
  if (longest <= 4) return 24;
  if (longest <= 6) return 19;
  if (longest <= 9) return 15;
  return 12;
}
