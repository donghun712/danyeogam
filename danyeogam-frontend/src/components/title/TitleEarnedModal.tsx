import { Modal } from "@/components/common/Modal";
import { Button } from "@/components/common/Button";
import type { Title } from "@/types/api";
import styles from "./TitleEarnedModal.module.css";

interface TitleEarnedModalProps {
  open: boolean;
  titles: Title[];
  onClose: () => void;
}

/**
 * 요청서 P1-7 — 칭호 획득 축하 피드백.
 * 새 칭호를 "이번 행동으로 처음 획득했는지"는 백엔드가 스탬프 인증 응답에
 * 이미 내려주는 newTitleIds로 확실히 알 수 있다(프론트가 임의로 추측하지 않음) —
 * 그래서 가능하다고 판단해 구현했다. 기존 Modal 컴포넌트를 그대로 재사용하고,
 * 복잡한 애니메이션 없이 정적인 메달 이미지 + 확인 버튼으로 최소 구현.
 */
export function TitleEarnedModal({ open, titles, onClose }: TitleEarnedModalProps) {
  if (titles.length === 0) return null;

  return (
    <Modal open={open} onClose={onClose}>
      <img src="/title-medal.png" alt="" className={styles.medal} />
      <p className="text-h3">새로운 칭호를 획득했어요!</p>
      <div className={styles.titleList}>
        {titles.map((title) => (
          <div key={title.id} className={styles.titleItem}>
            <p className={`text-h2 ${styles.titleName}`}>'{title.name}'</p>
            {title.description && (
              <p className={`text-caption ${styles.titleDescription}`}>
                {title.description}
              </p>
            )}
          </div>
        ))}
      </div>
      <Button fullWidth onClick={onClose}>
        확인
      </Button>
    </Modal>
  );
}
