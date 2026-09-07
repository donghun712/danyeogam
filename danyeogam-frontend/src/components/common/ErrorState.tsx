import { Button } from "./Button";
import styles from "./ErrorState.module.css";

interface ErrorStateProps {
  message: string;
  onRetry?: () => void;
  /**
   * true면 화면 전체가 아니라 한 영역(예: 주차장 섹션)만 실패한 것으로 표시한다.
   * 상태 명세서 8장 Partial Error: 관광지 상세는 정상 표시하고 주차장 영역만 오류로 둔다.
   */
  compact?: boolean;
}

export function ErrorState({ message, onRetry, compact = false }: ErrorStateProps) {
  return (
    <div className={compact ? styles.compactContainer : styles.container}>
      <p className="text-body">{message}</p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          다시 시도
        </Button>
      )}
    </div>
  );
}
