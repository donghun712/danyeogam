import { useState } from "react";
import { Button } from "./Button";
import { AppIcon } from "@/constants/icons";
import { useKakaoSdk } from "@/hooks/useKakaoSdk";
import { launchKakaoNavi } from "@/utils/kakaoNavi";
import type { NavigationTarget } from "@/types/api";
import styles from "./NavigationButton.module.css";

interface NavigationButtonProps {
  navigation: NavigationTarget;
}

const FAILURE_MESSAGE: Record<string, string> = {
  not_mobile: "카카오내비는 모바일 기기에서만 실행할 수 있어요.",
  sdk_not_ready: "카카오내비를 불러오는 중이에요. 잠시 후 다시 시도해 주세요.",
  unknown_error: "카카오내비를 실행하지 못했어요.",
};

/**
 * 디자인 시스템 17장 Navigation Button: Primary보다 낮은 Secondary 버튼.
 * NAV-01: 관광지 또는 주차장을 목적지로 전달 — navigation 객체는 백엔드 응답을 그대로 사용한다.
 */
export function NavigationButton({ navigation }: NavigationButtonProps) {
  useKakaoSdk(); // 버튼이 눌리기 전에 미리 로드해둔다
  const [failure, setFailure] = useState<string | null>(null);

  const handleClick = () => {
    const result = launchKakaoNavi(navigation);
    setFailure(result.ok ? null : FAILURE_MESSAGE[result.reason]);
  };

  return (
    <div className={styles.wrapper}>
      <Button variant="secondary" onClick={handleClick}>
        <AppIcon.navigation size={16} strokeWidth={2} aria-hidden="true" />
        길찾기
      </Button>
      {failure && <p className={`text-caption ${styles.failure}`}>{failure}</p>}
    </div>
  );
}
