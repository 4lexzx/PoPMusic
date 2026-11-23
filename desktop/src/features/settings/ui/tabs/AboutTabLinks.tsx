import type { ReactNode } from "react";
import { GithubFill, World2Line, TelegramFill } from "@/shared/ui/PopIcons";
import { useTranslation } from "@/languages";

interface AboutLink {
  id: string;
  url: string;
  label: string;
  icon: ReactNode;
}

const openUrl = (url: string) =>
  window.open(url, "_blank", "noopener,noreferrer");

export function AboutTabLinks() {
  const { t } = useTranslation();

  const links: AboutLink[] = [
    {
      id: "telegram",
      url: "https://t.me/popmusic_app",
      label: t("settings.about.telegram"),
      icon: <TelegramFill size={17} />,
    },
    {
      id: "github",
      url: "https://github.com/4lexzx/PoPMusic",
      label: t("settings.about.github"),
      icon: <GithubFill size={17} />,
    },
    {
      id: "website",
      url: "https://github.com/4lexzx/PoPMusic/releases",
      label: t("settings.about.website"),
      icon: <World2Line size={17} />,
    },
  ];

  return (
    <div className="flex flex-wrap items-center gap-[8px]">
      {links.map((link) => (
        <button
          key={link.id}
          type="button"
          onClick={() => openUrl(link.url)}
          className="flex h-[38px] items-center gap-[8px] rounded-item border-0 bg-fill px-[14px] text-label font-[500] text-text-primary transition-colors hover:bg-fill-hover"
        >
          {link.icon}
          {link.label}
        </button>
      ))}
    </div>
  );
}
