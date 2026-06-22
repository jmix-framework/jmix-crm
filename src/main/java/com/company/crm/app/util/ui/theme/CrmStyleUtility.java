package com.company.crm.app.util.ui.theme;

import io.jmix.flowui.theme.ThemeUtilityClasses;

@ThemeUtilityClasses(name = "CRM Utility")
public final class CrmStyleUtility {

    private CrmStyleUtility() {
    }

    public static final class Padding {

        public static final String MEDIUM = "p-m";

        private Padding() {
        }

        public static final class Top {
            public static final String MEDIUM = "pt-m";
            public static final String LARGE = "pt-l";

            private Top() {
            }
        }

        public static final class Bottom {
            public static final String SMALL = "pb-s";
            public static final String MEDIUM = "pb-m";
            public static final String LARGE = "pb-l";

            private Bottom() {
            }
        }

        public static final class Left {
            public static final String MEDIUM = "pl-m";

            private Left() {
            }
        }

        public static final class Right {
            public static final String MEDIUM = "pr-m";

            private Right() {
            }
        }

        public static final class Horizontal {
            public static final String MEDIUM = "px-m";

            private Horizontal() {
            }
        }

        public static final class Vertical {
            public static final String SMALL = "py-s";

            private Vertical() {
            }
        }
    }

    public static final class Margin {

        public static final String NONE = "m-0";
        public static final String AUTO = "m-auto";

        private Margin() {
        }

        public static final class Top {
            public static final String SMALL = "mt-s";
            public static final String MEDIUM = "mt-m";
            public static final String LARGE = "mt-l";

            private Top() {
            }
        }

        public static final class Bottom {
            public static final String XSMALL = "mb-xs";
            public static final String SMALL = "mb-s";
            public static final String MEDIUM = "mb-m";

            private Bottom() {
            }
        }

        public static final class Right {
            public static final String LARGE = "mr-l";

            private Right() {
            }
        }

        public static final class Left {
            public static final String MEDIUM = "ml-m";
            public static final String AUTO = "ml-auto";

            private Left() {
            }
        }

        public static final class Start {
            public static final String XSMALL = "ms-xs";

            private Start() {
            }
        }
    }

    public static final class FontSize {

        public static final String XSMALL = "text-xs";
        public static final String SMALL = "text-s";
        public static final String MEDIUM = "text-m";
        public static final String LARGE = "text-l";

        private FontSize() {
        }
    }

    public static final class FontWeight {

        public static final String THIN = "font-thin";
        public static final String LIGHT = "font-light";
        public static final String MEDIUM = "font-medium";
        public static final String SEMIBOLD = "font-semibold";
        public static final String BOLD = "font-bold";

        private FontWeight() {
        }
    }

    public static final class TextColor {

        public static final String PRIMARY = "text-primary";
        public static final String SECONDARY = "text-secondary";
        public static final String TERTIARY = "text-tertiary";
        public static final String BODY = "text-body";
        public static final String PRIMARY_CONTRAST = "text-primary-contrast";

        private TextColor() {
        }
    }

    public static final class Overflow {

        public static final String HIDDEN = "overflow-hidden";
        public static final String AUTO = "overflow-auto";

        private Overflow() {
        }
    }

    public static final class TextOverflow {

        public static final String ELLIPSIS = "text-ellipsis";

        private TextOverflow() {
        }
    }

    public static final class Whitespace {

        public static final String NOWRAP = "whitespace-nowrap";

        private Whitespace() {
        }
    }

    public static final class Flex {

        public static final String GROW = "flex-grow";

        private Flex() {
        }
    }

    public static final class BorderRadius {

        public static final String LARGE = "rounded-l";
        public static final String FULL = "rounded-full";

        private BorderRadius() {
        }
    }

    public static final class Border {

        public static final String ALL = "border";

        private Border() {
        }
    }

    public static final class IconSize {

        public static final String SMALL = "icon-s";

        private IconSize() {
        }
    }

    public static final class Background {

        public static final String TRANSPARENT = "bg-transparent";
        public static final String CONTRAST_5 = "bg-contrast-5";
        public static final String CONTRAST_10 = "bg-contrast-10";
        public static final String PRIMARY = "bg-primary";
        public static final String SUCCESS = "bg-success";
        public static final String WARNING = "bg-warning";
        public static final String ERROR_10 = "bg-error-10";

        private Background() {
        }
    }
}
